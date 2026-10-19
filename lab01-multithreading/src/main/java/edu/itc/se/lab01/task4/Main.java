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

        futures.add(pool.submit(new FakeDownload("http://slow", 0)));

        for (int i = 1; i <= 500; i++) {
            String url = "http://example.com/" + i;
            futures.add(pool.submit(new FakeDownload(url, i)));
        }

        int ok = 0;
        int failed = 0;
        int timedOut = 0;

        Future<String> slowFuture = futures.get(0);
        try {
            slowFuture.get(1, TimeUnit.SECONDS);
            ok++;
        } catch (TimeoutException e) {
            slowFuture.cancel(true); 
            timedOut++;
        } catch (ExecutionException e) {
            failed++;
        }

        for (int i = 1; i < futures.size(); i++) {
            Future<String> f = futures.get(i);
            try {
                f.get();
                ok++;
            } catch (ExecutionException e) {
                Throwable originalError = e.getCause();
                failed++;
            } catch (CancellationException e) {
            }
        }

        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("%-42s : ok=%d failed=%d timedOut=%d in %d ms%n",
                label, ok, failed, timedOut, ms);
    }

    public static void main(String[] args) throws Exception {
        final long LIMIT = 4_000_000;
        final int CHUNKS = 16;
        long t0 = System.nanoTime();
        long single = countPrimes(0, LIMIT);
        System.out.printf("single-threaded: %d primes in %d ms%n", single, (System.nanoTime() - t0) / 1_000_000);

        t0 = System.nanoTime();
        int cores = Runtime.getRuntime().availableProcessors();
        try (ExecutorService pool = Executors.newFixedThreadPool(cores)) {
            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < CHUNKS; i++) {
                long from = (long) i * LIMIT / CHUNKS;
                long to = (long) (i + 1) * LIMIT / CHUNKS;
                futures.add(pool.submit(new PrimeCounter(from, to)));
            }
            long total = 0;
            for (Future<Long> f : futures) {
                total += f.get();
            }
            System.out.printf("pool (%d threads): %d primes in %d ms%n", cores, total, (System.nanoTime() - t0) / 1_000_000);
        } 
        
        try (ExecutorService fixedPool = Executors.newFixedThreadPool(20)) {
            runDownloads(fixedPool, "downloads on fixed(20) platform threads");
        }

        try (ExecutorService virtualPool = Executors.newVirtualThreadPerTaskExecutor()) {
            runDownloads(virtualPool, "downloads on virtual threads");
        }
    }
}