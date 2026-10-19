package edu.itc.se.lab01.task4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    static long countPrimes(long from, long to) {
        long count = 0;
        for (long n = Math.max(from, 2); n < to; n++) {
            boolean prime = true;
            for (long d = 2; d * d <= n; d++) if (n % d == 0) { prime = false; break; }
            if (prime) count++;
        }
        return count;
    }

    private static void runDownloads(ExecutorService pool, String label) throws InterruptedException {
        long t0 = System.nanoTime();
        List<Future<String>> futures = new ArrayList<>();

        // Hint: Make the timed-out task the *first* you submit and wait on so the 1-second timeout is measurable[cite: 7]
        futures.add(pool.submit(new FakeDownload("http://slow", 0)));

        // Submit the remaining 500 tasks
        for (int i = 1; i <= 500; i++) {
            String url = "http://example.com/" + i;
            futures.add(pool.submit(new FakeDownload(url, i)));
        }

        int ok = 0;
        int failed = 0;
        int timedOut = 0;

        // Hint: Wait on the timed-out task first[cite: 7]
        Future<String> slowFuture = futures.get(0);
        try {
            slowFuture.get(1, TimeUnit.SECONDS);
            ok++;
        } catch (TimeoutException e) {
            slowFuture.cancel(true); // Cancel the slow task[cite: 5]
            timedOut++;
        } catch (ExecutionException e) {
            failed++;
        }

        // Wait on the rest of the tasks
        for (int i = 1; i < futures.size(); i++) {
            Future<String> f = futures.get(i);
            try {
                f.get();
                ok++;
            } catch (ExecutionException e) {
                // Hint: ExecutionException.getCause() holds the original IOException[cite: 7]
                Throwable originalError = e.getCause();
                failed++;
            } catch (CancellationException e) {
                // Ignored if cancelled
            }
        }

        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("%-42s : ok=%d failed=%d timedOut=%d in %d ms%n",
                label, ok, failed, timedOut, ms);
    }

    public static void main(String[] args) throws Exception {
        final long LIMIT = 4_000_000;
        final int CHUNKS = 16;

        // 1. Single-threaded CPU prime count
        long t0 = System.nanoTime();
        long single = countPrimes(0, LIMIT);
        System.out.printf("single-threaded: %d primes in %d ms%n", single, (System.nanoTime() - t0) / 1_000_000);

        // 2. Thread pool CPU prime count using exact chunk boundaries from hints[cite: 7]
        t0 = System.nanoTime();
        int cores = Runtime.getRuntime().availableProcessors();
        try (ExecutorService pool = Executors.newFixedThreadPool(cores)) {
            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < CHUNKS; i++) {
                // Hint formula for chunk boundaries[cite: 7]
                long from = (long) i * LIMIT / CHUNKS;
                long to = (long) (i + 1) * LIMIT / CHUNKS;
                futures.add(pool.submit(new PrimeCounter(from, to)));
            }
            long total = 0;
            for (Future<Long> f : futures) {
                total += f.get();
            }
            System.out.printf("pool (%d threads): %d primes in %d ms%n", cores, total, (System.nanoTime() - t0) / 1_000_000);
        } // Try-with-resources guarantees the pool is shut down, preventing active non-daemon threads from hanging the JVM[cite: 5, 7]

        // 3 & 4. I/O-Bound part: Fixed platform pool vs Virtual threads[cite: 5, 7]
        try (ExecutorService fixedPool = Executors.newFixedThreadPool(20)) {
            runDownloads(fixedPool, "downloads on fixed(20) platform threads");
        }

        try (ExecutorService virtualPool = Executors.newVirtualThreadPerTaskExecutor()) {
            runDownloads(virtualPool, "downloads on virtual threads");
        }
    }
}