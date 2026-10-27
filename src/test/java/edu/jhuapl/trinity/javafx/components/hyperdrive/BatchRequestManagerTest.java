package edu.jhuapl.trinity.javafx.components.hyperdrive;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class BatchRequestManagerTest {
    @Test
    void limitsConcurrentRequestsAndSpacesLaunches() throws Exception {
        var ids = new AtomicInteger();
        var active = new AtomicInteger();
        var peak = new AtomicInteger();
        var starts = Collections.synchronizedList(new ArrayList<Long>());
        var done = new CountDownLatch(5);
        var timer = Executors.newScheduledThreadPool(2);
        @SuppressWarnings("unchecked")
        BatchRequestManager<List<Integer>>[] managers = new BatchRequestManager[1];
        try {
            managers[0] = new BatchRequestManager<>(2, 1000, 0, ids::incrementAndGet,
                ids::incrementAndGet, (batch, number, id) -> () -> {
                    peak.accumulateAndGet(active.incrementAndGet(), Math::max);
                    starts.add(System.nanoTime());
                    timer.schedule(() -> {
                        active.decrementAndGet();
                        managers[0].completeSuccess(id, number, batch, 0);
                    }, 75, TimeUnit.MILLISECONDS);
                }, result -> done.countDown());
            managers[0].setRequestDelayMillis(35);
            List<List<Integer>> batches = new ArrayList<>();
            for (int i = 0; i < 23; i += 5) {
                List<Integer> batch = new ArrayList<>();
                for (int j = i; j < Math.min(i + 5, 23); j++) batch.add(j);
                batches.add(batch);
            }
            assertTrue(managers[0].enqueue(batches));
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertEquals(5, starts.size());
            assertTrue(peak.get() <= 2);
            for (int i = 1; i < starts.size(); i++) {
                assertTrue(TimeUnit.NANOSECONDS.toMillis(starts.get(i) - starts.get(i - 1)) >= 20);
            }
        } finally {
            if (managers[0] != null) managers[0].shutdown();
            timer.shutdownNow();
        }
    }

    @Test
    void retriesHaveNewIdsAndLateCompletionIsIgnored() throws Exception {
        var ids = new AtomicInteger();
        var attempts = Collections.synchronizedList(new ArrayList<Integer>());
        var done = new CountDownLatch(1);
        var status = new AtomicReference<String>();
        @SuppressWarnings("unchecked")
        BatchRequestManager<String>[] managers = new BatchRequestManager[1];
        try {
            managers[0] = new BatchRequestManager<>(1, 55, 1, ids::incrementAndGet,
                ids::incrementAndGet, (batch, number, id) -> () -> {
                    attempts.add(id);
                    if (attempts.size() == 2) managers[0].completeSuccess(id, number, batch, 0);
                }, result -> { status.set(result.status().name()); done.countDown(); });
            assertTrue(managers[0].enqueue(List.of("chunk")));
            assertTrue(done.await(2, TimeUnit.SECONDS));
            assertEquals(2, attempts.size());
            assertNotEquals(attempts.get(0), attempts.get(1));
            managers[0].completeFailure(attempts.get(0), 1, "chunk", 0, new IOException("late"));
            Thread.sleep(40);
            assertEquals(1, managers[0].getBatchesCompleted());
            assertEquals("SUCCESS", status.get());
        } finally {
            if (managers[0] != null) managers[0].shutdown();
        }
    }
}
