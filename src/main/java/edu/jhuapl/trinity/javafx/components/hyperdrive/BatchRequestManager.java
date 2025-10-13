package edu.jhuapl.trinity.javafx.components.hyperdrive;

/**
 *
 * @author Sean Phillips
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Generic manager for batching and throttling REST requests with retries and
 * timeouts.
 *
 * @param <T> T = your batch/request type (e.g. List<EmbeddingsImageListItem>)
 *
 */
public class BatchRequestManager<T> {

    private static final Logger LOG = LoggerFactory.getLogger(BatchRequestManager.class);

    private final Queue<BatchWrapper<T>> pendingQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);
    private long requestDelayMillis = 0;
    private int maxInFlight;
    private long timeoutMillis;
    private int maxRetries;
    private int batchesCompleted = 0;
    private int totalBatches = 0;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<Integer, ScheduledFuture<?>> timeouts = new ConcurrentHashMap<>();
    private final Map<Integer, Long> batchStartTimes = new ConcurrentHashMap<>();
    private final List<Long> batchDurations = Collections.synchronizedList(new ArrayList<>());
    private final Map<Integer, Long> batchEndDurations = new ConcurrentHashMap<>();

    private final Supplier<Integer> requestIdSupplier;
    private final Supplier<Integer> batchNumberSupplier;
    private final TriFunction<T, Integer, Integer, Runnable> taskFactory; // (batch, batchNumber, reqId) -> Runnable
    private final Consumer<BatchResult<T>> onComplete;

    public interface BatchResult<T> {

        int getBatchNumber();

        int getRequestId();

        T getBatch();

        Status getStatus();

        int getRetryCount();

        Exception getException();

        enum Status {
            SUCCESS, FAILURE, TIMEOUT
        }
    }

    // Internal wrapper
    private static class BatchWrapper<T> {

        final T batch;
        final int batchNumber;
        final int requestId; // Typically per batch, but you can expand to per-item if you want
        int retries;

        BatchWrapper(T batch, int batchNumber, int requestId) {
            this.batch = batch;
            this.batchNumber = batchNumber;
            this.requestId = requestId;
            this.retries = 0;
        }
    }

    // TriFunction interface
    @FunctionalInterface
    public interface TriFunction<A, B, C, R> {

        R apply(A a, B b, C c);
    }

    public BatchRequestManager(
        int maxInFlight,
        long timeoutMillis,
        int maxRetries,
        Supplier<Integer> batchNumberSupplier,
        Supplier<Integer> requestIdSupplier,
        TriFunction<T, Integer, Integer, Runnable> taskFactory, // (batch, batchNumber, reqId) -> Runnable
        Consumer<BatchResult<T>> onComplete
    ) {
        this.maxInFlight = maxInFlight;
        this.timeoutMillis = timeoutMillis;
        this.maxRetries = maxRetries;
        this.batchNumberSupplier = batchNumberSupplier;
        this.requestIdSupplier = requestIdSupplier;
        this.taskFactory = taskFactory;
        this.onComplete = onComplete;
    }

    public void enqueue(Collection<T> batches) {
        totalBatches = batches.size();
        batchesCompleted = 0;
        batchDurations.clear();
        batchEndDurations.clear();
        batchStartTimes.clear();
        for (T batch : batches) {
            int batchNum = batchNumberSupplier.get();
            int reqId = requestIdSupplier.get();
            pendingQueue.add(new BatchWrapper<>(batch, batchNum, reqId));
        }
        // Prime the pipeline: fill all concurrency slots now
        initialDispatch();
    }

    public void stopAndClear() {
        pendingQueue.clear();
        // Cancel all outstanding timeouts
        for (ScheduledFuture<?> future : timeouts.values()) {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }
        timeouts.clear();
        clearCounters();
    }

    public void clearCounters() {
        //clear batch durations, starts, etc.
        batchStartTimes.clear();
        batchEndDurations.clear();
        batchDurations.clear();
        //reset counters
        inFlight.set(0);
        batchesCompleted = 0;
        totalBatches = 0;
        LOG.warn("BatchRequestManager: All queued batches and timers stopped and cleared by user.");
    }

    // Call this from enqueue() only
    private void initialDispatch() {
        while (inFlight.get() < maxInFlight && !pendingQueue.isEmpty()) {
            scheduleNextBatch();
        }
    }

    // Call this from completion/failure/timeout
    private void dispatch() {
        while (inFlight.get() < maxInFlight && !pendingQueue.isEmpty()) {
            scheduleNextBatch();
        }
    }

    private void scheduleNextBatch() {
        BatchWrapper<T> wrapper = pendingQueue.poll();
        if (wrapper != null) {
            inFlight.incrementAndGet();
            LOG.info("Dispatching batch {} (batchNumber={}) (retry #{})", wrapper.requestId, wrapper.batchNumber, wrapper.retries);

            // Timeout for this batch
            ScheduledFuture<?> timeout = scheduler.schedule(() -> {
                onTimeout(wrapper);
            }, timeoutMillis, TimeUnit.MILLISECONDS);
            timeouts.put(wrapper.requestId, timeout);

            // Schedule the batch with delay
            scheduler.schedule(() -> {
                Runnable task = taskFactory.apply(wrapper.batch, wrapper.batchNumber, wrapper.requestId);
                try {
                    task.run();
                } catch (Exception ex) {
                    LOG.error("Batch {} (batchNumber={}) threw exception: {}", wrapper.requestId, wrapper.batchNumber, ex.toString());
                    handleFailure(wrapper, ex);
                }
            }, requestDelayMillis, TimeUnit.MILLISECONDS);

            long now = System.currentTimeMillis();
            batchStartTimes.put(wrapper.requestId, now);
        }
    }

    public void completeSuccess(int requestId, int batchNumber, T batch, int retries) {
        handleCompletion(requestId, batchNumber, batch, retries, BatchResult.Status.SUCCESS, null);
    }

    public void completeFailure(int requestId, int batchNumber, T batch, int retries, Exception ex) {
        handleCompletion(requestId, batchNumber, batch, retries, BatchResult.Status.FAILURE, ex);
    }

    private void onTimeout(BatchWrapper<T> wrapper) {
        LOG.warn("Batch {} (batchNumber={}) timed out after {}ms (retry #{})",
            wrapper.requestId, wrapper.batchNumber, timeoutMillis, wrapper.retries);
        handleFailure(wrapper, null); // handleFailure will call dispatch()
    }

    private void handleCompletion(int requestId, int batchNumber, T batch, int retries, BatchResult.Status status, Exception ex) {
        ScheduledFuture<?> timeout = timeouts.remove(requestId);
        if (timeout != null) {
            timeout.cancel(false);
        }
        inFlight.decrementAndGet();
        batchesCompleted++;

        //Compute and store batch duration
        Long startTime = batchStartTimes.remove(requestId);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            batchDurations.add(duration); // keep this as "most recent"
            LOG.info("Batch {} completed in {} ms (avg: {} ms)", requestId, duration, getAvgBatchDurationMillis());
            batchEndDurations.put(requestId, duration);
        }

        if (onComplete != null) {
            onComplete.accept(new BatchResultImpl<>(requestId, batchNumber, batch, status, retries, ex));
        }
        dispatch();
    }

    private void handleFailure(BatchWrapper<T> wrapper, Exception ex) {
        timeouts.remove(wrapper.requestId);
        inFlight.decrementAndGet();
        if (wrapper.retries < maxRetries) {
            wrapper.retries++;
            LOG.info("Retrying batch {} (batchNumber={}) (retry #{})", wrapper.requestId, wrapper.batchNumber, wrapper.retries);
            pendingQueue.add(wrapper);
        } else {
            LOG.error("Batch {} (batchNumber={}) failed after {} retries", wrapper.requestId, wrapper.batchNumber, wrapper.retries);
            if (onComplete != null) {
                onComplete.accept(new BatchResultImpl<>(wrapper.requestId, wrapper.batchNumber, wrapper.batch, BatchResult.Status.FAILURE, wrapper.retries, ex));
            }
        }
        dispatch();
    }

    public double getAvgBatchDurationMillis() {
        if (batchDurations.isEmpty()) return 0;
        return batchDurations.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    public long getTotalBatchDurationMillis() {
        if (batchDurations.isEmpty()) return 0;
        return batchDurations.stream().mapToLong(Long::longValue).sum();
    }

    public long getLastBatchDurationMillis() {
        if (batchDurations.isEmpty()) return 0;
        return batchDurations.get(batchDurations.size() - 1);
    }

    public long getBatchDurationByID(int id) {
        return batchEndDurations.getOrDefault(id, 0L);
    }

    public long getRequestDelayMillis() {
        return requestDelayMillis;
    }

    public void setRequestDelayMillis(long requestDelayMillis) {
        this.requestDelayMillis = requestDelayMillis;
    }

    public int getInFlight() {
        return inFlight.get();
    }

    public int getMaxInFlight() {
        return maxInFlight;
    }

    public void setMaxInFlight(int maxInFlight) {
        this.maxInFlight = maxInFlight;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * @return the batchesCompleted
     */
    public int getBatchesCompleted() {
        return batchesCompleted;
    }

    /**
     * @param batchesCompleted the batchesCompleted to set
     */
    public void setBatchesCompleted(int batchesCompleted) {
        this.batchesCompleted = batchesCompleted;
    }

    /**
     * @return the totalBatches
     */
    public int getTotalBatches() {
        return totalBatches;
    }

    /**
     * @param totalBatches the totalBatches to set
     */
    public void setTotalBatches(int totalBatches) {
        this.totalBatches = totalBatches;
    }

    private static class BatchResultImpl<T> implements BatchResult<T> {

        private final int requestId;
        private final int batchNumber;
        private final T batch;
        private final Status status;
        private final int retryCount;
        private final Exception ex;

        BatchResultImpl(int requestId, int batchNumber, T batch, Status status, int retryCount, Exception ex) {
            this.requestId = requestId;
            this.batchNumber = batchNumber;
            this.batch = batch;
            this.status = status;
            this.retryCount = retryCount;
            this.ex = ex;
        }

        @Override
        public int getRequestId() {
            return requestId;
        }

        @Override
        public int getBatchNumber() {
            return batchNumber;
        }

        @Override
        public T getBatch() {
            return batch;
        }

        @Override
        public Status getStatus() {
            return status;
        }

        @Override
        public int getRetryCount() {
            return retryCount;
        }

        @Override
        public Exception getException() {
            return ex;
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
