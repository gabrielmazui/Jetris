package matches;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class GameLoopManager {

    private static final int TICK_RATE_MS = 16;
    private static final int INLINE_THRESHOLD = 10;

    private static final ScheduledExecutorService heartbeat =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "game-loop-heartbeat");
            t.setDaemon(true);
            return t;
        });

    private static final ExecutorService workers =
        Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r, "game-loop-worker");
                t.setDaemon(true);
                return t;
            }
        );

    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final AtomicLong tickCounter = new AtomicLong(0);

    public static void start() {
        if (!running.compareAndSet(false, true)) {
            return; 
        }
        heartbeat.scheduleAtFixedRate(
            GameLoopManager::safeTick,
            0,
            TICK_RATE_MS,
            TimeUnit.MILLISECONDS
        );
    }

    private static void safeTick() {
        try {
            processAllTicks();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        tickCounter.incrementAndGet();
    }

    private static void processAllTicks() {
        Collection<MatchSession> activeMatches = MatchManager.getAllActiveMatches();
        if (activeMatches.isEmpty()) {
            return;
        }

        if (activeMatches.size() <= INLINE_THRESHOLD) {
            for (MatchSession session : activeMatches) {
                tickSessionSafely(session);
            }
            return;
        }

        CompletableFuture<?>[] futures = activeMatches.stream()
            .map(session -> CompletableFuture.runAsync(() -> tickSessionSafely(session), workers))
            .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();
    }

    private static void tickSessionSafely(MatchSession session) {
        try {
            if (session.isActive()) {
                session.tick();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void shutdown() {
        running.set(false);
        heartbeat.shutdown();
        workers.shutdown();
        try {
            if (!heartbeat.awaitTermination(2, TimeUnit.SECONDS)) {
                heartbeat.shutdownNow();
            }
            if (!workers.awaitTermination(2, TimeUnit.SECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException e) {
            heartbeat.shutdownNow();
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}