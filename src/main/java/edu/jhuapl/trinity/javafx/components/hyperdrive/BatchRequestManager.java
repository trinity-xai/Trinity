package edu.jhuapl.trinity.javafx.components.hyperdrive;

/**
 *
 * @author Sean Phillips
 */

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic manager for batching and throttling REST requests with retries and timeouts.
 * @param <T> T = your batch/request type (e.g. List<EmbeddingsImageListItem>)
 * 
 */
public class BatchRequestManager<T> {
    private static final Logger LOG = LoggerFactory.getLogger(BatchRequestManager.class);

    private final Queue<BatchWrapper<T>> pendingQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);
    private final int maxInFlight;
    private final long timeoutMillis;
    private final int maxRetries;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<Integer, ScheduledFuture<?>> timeouts = new ConcurrentHashMap<>();

    private final BiFunction<T, Integer, Runnable> taskFactory; // (batch, requestId) -> Runnable/Task
    private final Consumer<BatchResult<T>> onComplete; // Info on batch completion/failure/timeout
    private final Supplier<Integer> requestIdSupplier;

    // For logging or UI updates
    public interface BatchResult<T> {
        int getRequestId();
        T getBatch();
        Status getStatus();
        int getRetryCount();
        Exception getException();

        enum Status { SUCCESS, FAILURE, TIMEOUT }
    }

    // Internal wrapper to track retries, etc.
    private static class BatchWrapper<T> {
        final T batch;
        int retries;
        final int requestId;

        BatchWrapper(T batch, int requestId) {
            this.batch = batch;
            this.retries = 0;
            this.requestId = requestId;
        }
    }

    public BatchRequestManager(
            int maxInFlight,
            long timeoutMillis,
            int maxRetries,
            Supplier<Integer> requestIdSupplier,
            BiFunction<T, Integer, Runnable> taskFactory,
            Consumer<BatchResult<T>> onComplete
    ) {
        this.maxInFlight = maxInFlight;
        this.timeoutMillis = timeoutMillis;
        this.maxRetries = maxRetries;
        this.requestIdSupplier = requestIdSupplier;
        this.taskFactory = taskFactory;
        this.onComplete = onComplete;
    }

    public void enqueue(Collection<T> batches) {
        for (T batch : batches) {
            int reqId = requestIdSupplier.get();
            pendingQueue.add(new BatchWrapper<>(batch, reqId));
        }
        dispatch();
    }

    private void dispatch() {
        while (inFlight.get() < maxInFlight && !pendingQueue.isEmpty()) {
            BatchWrapper<T> wrapper = pendingQueue.poll();
            if (wrapper != null) {
                inFlight.incrementAndGet();
                LOG.info("Dispatching batch {} (retry #{})", wrapper.requestId, wrapper.retries);
                ScheduledFuture<?> timeout = scheduler.schedule(() -> {
                    onTimeout(wrapper);
                }, timeoutMillis, TimeUnit.MILLISECONDS);
                timeouts.put(wrapper.requestId, timeout);

                Runnable task = taskFactory.apply(wrapper.batch, wrapper.requestId);
                new Thread(() -> {
                    try {
                        task.run();
                        // On success, complete() should be called externally
                    } catch (Exception ex) {
                        LOG.error("Batch {} threw exception: {}", wrapper.requestId, ex.toString());
                        handleFailure(wrapper, ex);
                    }
                }, "BatchRequest-" + wrapper.requestId).start();
            }
        }
    }

    // Call this from your Task when a batch completes successfully
    public void completeSuccess(int requestId, T batch, int retries) {
        handleCompletion(requestId, batch, retries, BatchResult.Status.SUCCESS, null);
    }

    // Call this from your Task when a batch fails explicitly (not via timeout)
    public void completeFailure(int requestId, T batch, int retries, Exception ex) {
        handleCompletion(requestId, batch, retries, BatchResult.Status.FAILURE, ex);
    }

    // Only call directly from timeout handler
    private void onTimeout(BatchWrapper<T> wrapper) {
        LOG.warn("Batch {} timed out after {}ms (retry #{})", wrapper.requestId, timeoutMillis, wrapper.retries);
        handleFailure(wrapper, null); // Ex == null means timeout
    }

    private void handleFailure(BatchWrapper<T> wrapper, Exception ex) {
        timeouts.remove(wrapper.requestId);
        inFlight.decrementAndGet();
        if (wrapper.retries < maxRetries) {
            wrapper.retries++;
            LOG.info("Retrying batch {} (retry #{})", wrapper.requestId, wrapper.retries);
            pendingQueue.add(wrapper);
        } else {
            LOG.error("Batch {} failed after {} retries", wrapper.requestId, wrapper.retries);
            if (onComplete != null) {
                onComplete.accept(new BatchResultImpl<>(wrapper.requestId, wrapper.batch, BatchResult.Status.FAILURE, wrapper.retries, ex));
            }
        }
        dispatch();
    }

    private void handleCompletion(int requestId, T batch, int retries, BatchResult.Status status, Exception ex) {
        ScheduledFuture<?> timeout = timeouts.remove(requestId);
        if (timeout != null) timeout.cancel(false);
        inFlight.decrementAndGet();
        if (onComplete != null) {
            onComplete.accept(new BatchResultImpl<>(requestId, batch, status, retries, ex));
        }
        dispatch();
    }

    private static class BatchResultImpl<T> implements BatchResult<T> {
        private final int requestId;
        private final T batch;
        private final Status status;
        private final int retryCount;
        private final Exception ex;
        BatchResultImpl(int requestId, T batch, Status status, int retryCount, Exception ex) {
            this.requestId = requestId;
            this.batch = batch;
            this.status = status;
            this.retryCount = retryCount;
            this.ex = ex;
        }
        @Override
        public int getRequestId() { return requestId; }
        @Override
        public T getBatch() { return batch; }
        @Override
        public Status getStatus() { return status; }
        @Override
        public int getRetryCount() { return retryCount; }
        @Override
        public Exception getException() { return ex; }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}