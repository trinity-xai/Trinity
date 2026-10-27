package edu.jhuapl.trinity.javafx.components.hyperdrive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Bounded asynchronous batch dispatcher. All mutable dispatch state is owned by
 * one scheduler thread. A request occupies a slot from its actual launch until
 * completion/timeout; retries receive fresh request IDs, so late HTTP responses
 * cannot complete a newer attempt.
 */
public class BatchRequestManager<T> {
    private static final Logger LOG = LoggerFactory.getLogger(BatchRequestManager.class);

    private final Queue<BatchWrapper<T>> pendingQueue = new ArrayDeque<>();
    private final Map<Integer, Attempt<T>> active = new HashMap<>();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Trinity Batch Request Dispatcher");
        t.setDaemon(true);
        return t;
    });
    private final List<Long> batchDurations = Collections.synchronizedList(new ArrayList<>());
    private final Map<Integer, Long> batchEndDurations = new java.util.concurrent.ConcurrentHashMap<>();
    private final Supplier<Integer> requestIdSupplier;
    private final Supplier<Integer> batchNumberSupplier;
    private final TriFunction<T, Integer, Integer, Runnable> taskFactory;
    private final Consumer<BatchResult<T>> onComplete;
    private volatile int maxInFlight;
    private volatile long timeoutMillis;
    private volatile long requestDelayMillis;
    private volatile int maxRetries;
    private volatile int batchesCompleted;
    private volatile int totalBatches;
    private volatile boolean busy;
    private volatile long generation;
    private long nextLaunchNanos;
    private long lastActualLaunchNanos;

    public interface BatchResult<T> {
        int batchNumber();
        int requestId();
        T batch();
        Status status();
        int retryCount();
        Exception exception();
        enum Status { SUCCESS, FAILURE, TIMEOUT }
    }

    private static class BatchWrapper<T> {
        final T batch;
        final int batchNumber;
        int retries;
        BatchWrapper(T batch, int batchNumber) {
            this.batch = batch;
            this.batchNumber = batchNumber;
        }
    }

    private static class Attempt<T> {
        final BatchWrapper<T> wrapper;
        final int requestId;
        ScheduledFuture<?> launch;
        ScheduledFuture<?> timeout;
        long startedNanos;
        Attempt(BatchWrapper<T> wrapper, int requestId) {
            this.wrapper = wrapper;
            this.requestId = requestId;
        }
    }

    @FunctionalInterface
    public interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    public BatchRequestManager(int maxInFlight, long timeoutMillis, int maxRetries,
                               Supplier<Integer> batchNumberSupplier,
                               Supplier<Integer> requestIdSupplier,
                               TriFunction<T, Integer, Integer, Runnable> taskFactory,
                               Consumer<BatchResult<T>> onComplete) {
        setMaxInFlight(maxInFlight);
        setTimeoutMillis(timeoutMillis);
        setMaxRetries(maxRetries);
        this.batchNumberSupplier = batchNumberSupplier;
        this.requestIdSupplier = requestIdSupplier;
        this.taskFactory = taskFactory;
        this.onComplete = onComplete;
    }

    /** Must not enqueue a second job while the previous one is still running. */
    public synchronized boolean enqueue(Collection<T> batches) {
        if (busy) return false;
        if (batches.isEmpty()) return true;
        busy = true;
        long jobGeneration = ++generation;
        scheduler.execute(() -> {
            if (generation != jobGeneration) return;
            batchDurations.clear();
            batchEndDurations.clear();
            batchesCompleted = 0;
            totalBatches = batches.size();
            nextLaunchNanos = System.nanoTime();
            lastActualLaunchNanos = 0;
            for (T batch : batches) {
                pendingQueue.add(new BatchWrapper<>(batch, batchNumberSupplier.get()));
            }
            dispatch();
        });
        return true;
    }

    /** Invalidate all queued launches and late callbacks, including after a new job starts. */
    public synchronized void stopAndClear() {
        ++generation;
        busy = false;
        scheduler.execute(() -> {
            pendingQueue.clear();
            for (Attempt<T> attempt : active.values()) {
                if (attempt.launch != null) attempt.launch.cancel(false);
                if (attempt.timeout != null) attempt.timeout.cancel(false);
            }
            active.clear();
            clearCountersInternal();
        });
    }

    /** Kept for existing callers; do not reset concurrency while requests are active. */
    public void clearCounters() {
        scheduler.execute(() -> {
            if (!busy) clearCountersInternal();
        });
    }

    private void clearCountersInternal() {
        inFlight.set(0);
        batchesCompleted = 0;
        totalBatches = 0;
        batchDurations.clear();
        batchEndDurations.clear();
    }

    private void dispatch() {
        while (active.size() < maxInFlight && !pendingQueue.isEmpty()) {
            BatchWrapper<T> wrapper = pendingQueue.remove();
            int reqId = requestIdSupplier.get();
            Attempt<T> attempt = new Attempt<>(wrapper, reqId);
            active.put(reqId, attempt);
            inFlight.set(active.size());
            long now = System.nanoTime();
            long scheduledAt = Math.max(now, nextLaunchNanos);
            nextLaunchNanos = scheduledAt + TimeUnit.MILLISECONDS.toNanos(requestDelayMillis);
            long waitNanos = Math.max(0, scheduledAt - now);
            long epoch = generation;
            attempt.launch = scheduler.schedule(() -> launchAttempt(attempt, epoch),
                waitNanos, TimeUnit.NANOSECONDS);
        }
        if (busy && active.isEmpty() && pendingQueue.isEmpty()) busy = false;
    }

    private void launchAttempt(Attempt<T> attempt, long epoch) {
        if (generation != epoch || active.get(attempt.requestId) != attempt) return;
        long now = System.nanoTime();
        long delayNanos = TimeUnit.MILLISECONDS.toNanos(requestDelayMillis);
        if (lastActualLaunchNanos != 0 && now - lastActualLaunchNanos < delayNanos) {
            long wait = delayNanos - (now - lastActualLaunchNanos);
            attempt.launch = scheduler.schedule(() -> launchAttempt(attempt, epoch),
                wait, TimeUnit.NANOSECONDS);
            return;
        }
        lastActualLaunchNanos = now;
        attempt.startedNanos = now;
        attempt.timeout = scheduler.schedule(() -> finish(attempt.requestId,
            BatchResult.Status.TIMEOUT, new java.util.concurrent.TimeoutException("Batch request timed out"), null),
            timeoutMillis, TimeUnit.MILLISECONDS);
        try {
            taskFactory.apply(attempt.wrapper.batch, attempt.wrapper.batchNumber, attempt.requestId).run();
        } catch (Exception ex) {
            finish(attempt.requestId, BatchResult.Status.FAILURE, ex, null);
        }
    }

    public void completeSuccess(int requestId, int batchNumber, T batch, int retries) {
        completeSuccess(requestId, batchNumber, batch, retries, null);
    }

    /** successAction is run only if this exact attempt is still active. */
    public void completeSuccess(int requestId, int batchNumber, T batch, int retries, Runnable successAction) {
        scheduler.execute(() -> finish(requestId, BatchResult.Status.SUCCESS, null, successAction));
    }

    public void completeFailure(int requestId, int batchNumber, T batch, int retries, Exception ex) {
        scheduler.execute(() -> finish(requestId, BatchResult.Status.FAILURE, ex, null));
    }

    private void finish(int requestId, BatchResult.Status status, Exception exception, Runnable successAction) {
        Attempt<T> attempt = active.remove(requestId);
        if (attempt == null) return; // timeout, stop, or prior callback already completed this attempt
        if (attempt.launch != null) attempt.launch.cancel(false);
        if (attempt.timeout != null) attempt.timeout.cancel(false);
        inFlight.set(active.size());
        BatchWrapper<T> wrapper = attempt.wrapper;
        if (attempt.startedNanos != 0) {
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - attempt.startedNanos);
            batchDurations.add(duration);
            batchEndDurations.put(requestId, duration);
        }
        if (status == BatchResult.Status.SUCCESS) {
            if (successAction != null) successAction.run();
        } else if (wrapper.retries < maxRetries) {
            ++wrapper.retries;
            LOG.warn("Batch {} attempt {} failed ({}); retrying", wrapper.batchNumber, requestId, status);
            pendingQueue.add(wrapper);
            dispatch();
            return;
        } else {
            LOG.error("Batch {} failed after {} retries: {}", wrapper.batchNumber, wrapper.retries, exception);
        }
        ++batchesCompleted;
        if (onComplete != null) {
            onComplete.accept(new BatchResultImpl<>(requestId, wrapper.batchNumber, wrapper.batch,
                status, wrapper.retries, exception));
        }
        dispatch();
    }

    public double getAvgBatchDurationMillis() {
        synchronized (batchDurations) {
            return batchDurations.stream().mapToLong(Long::longValue).average().orElse(0);
        }
    }
    public long getTotalBatchDurationMillis() {
        synchronized (batchDurations) {
            return batchDurations.stream().mapToLong(Long::longValue).sum();
        }
    }
    public long getLastBatchDurationMillis() {
        synchronized (batchDurations) {
            return batchDurations.isEmpty() ? 0 : batchDurations.get(batchDurations.size() - 1);
        }
    }
    public long getBatchDurationByID(int id) { return batchEndDurations.getOrDefault(id, 0L); }
    public long getRequestDelayMillis() { return requestDelayMillis; }
    public void setRequestDelayMillis(long ms) { requestDelayMillis = Math.max(0, ms); }
    public int getInFlight() { return inFlight.get(); }
    public boolean isBusy() { return busy; }
    public int getMaxInFlight() { return maxInFlight; }
    public void setMaxInFlight(int limit) {
        if (limit < 1) throw new IllegalArgumentException("maxInFlight must be >= 1");
        maxInFlight = limit;
        scheduler.execute(this::dispatch);
    }
    public long getTimeoutMillis() { return timeoutMillis; }
    public void setTimeoutMillis(long ms) {
        if (ms < 1) throw new IllegalArgumentException("timeoutMillis must be >= 1");
        timeoutMillis = ms;
    }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int retries) {
        if (retries < 0) throw new IllegalArgumentException("maxRetries must be >= 0");
        maxRetries = retries;
    }
    public int getBatchesCompleted() { return batchesCompleted; }
    public void setBatchesCompleted(int completed) { batchesCompleted = completed; }
    public int getTotalBatches() { return totalBatches; }
    public void setTotalBatches(int total) { totalBatches = total; }

    private record BatchResultImpl<T>(int requestId, int batchNumber, T batch, Status status,
                                      int retryCount, Exception exception) implements BatchResult<T> { }

    public void shutdown() { scheduler.shutdownNow(); }
}
