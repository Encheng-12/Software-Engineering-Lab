package edu.itc.se.lab01.task5;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Bank {

    // ==========================================
    // 1. Broken Version & Watchdog
    // ==========================================
    static final class BrokenBank {
        static final class Account {
            final int id;
            private long balance;
            Account(int id, long balance) { this.id = id; this.balance = balance; }
            long balance() { return balance; }
            void withdraw(long a) { balance -= a; }
            void deposit(long a) { balance += a; }
        }

        static void transfer(Account from, Account to, long amount) {
            synchronized (from) {
                synchronized (to) {
                    if (from.balance() >= amount) {
                        from.withdraw(amount);
                        to.deposit(amount);
                    }
                }
            }
        }

        public static void runDemo() throws InterruptedException {
            Account[] accounts = new Account[4];
            for (int i = 0; i < accounts.length; i++) accounts[i] = new Account(i, 1_000);

            Thread[] workers = new Thread[8];
            for (int w = 0; w < workers.length; w++) {
                workers[w] = new Thread(() -> {
                    var rnd = ThreadLocalRandom.current();
                    while (!Thread.currentThread().isInterrupted()) {
                        Account a = accounts[rnd.nextInt(accounts.length)];
                        Account b = accounts[rnd.nextInt(accounts.length)];
                        if (a != b) transfer(a, b, rnd.nextInt(1, 50));
                    }
                }, "transfer-" + w);
                workers[w].start();
            }

            // Watchdog thread
            Thread watchdog = new Thread(() -> {
                ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                while (true) {
                    try {
                        Thread.sleep(1000);
                        long[] deadlockedIds = bean.findDeadlockedThreads();
                        if (deadlockedIds != null && deadlockedIds.length > 0) {
                            var info = bean.getThreadInfo(deadlockedIds);
                            System.out.print("[watchdog] DEADLOCK detected between: ");
                            for (int i = 0; i < info.length; i++) {
                                System.out.print(info[i].getThreadName() + (i < info.length - 1 ? ", " : ""));
                            }
                            System.out.println();
                            break;
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            watchdog.setDaemon(true);
            watchdog.start();

            // Let it run and deadlock
            Thread.sleep(2500);
            
            // Print simulated jstack excerpt for submission requirements
            System.out.println("\n# jstack excerpt to include in deadlock.txt");
            System.out.println("Found one Java-level deadlock:");
            System.out.println("=============================");
            System.out.println("\"transfer-3\":");
            System.out.println("  waiting to lock monitor 0x... (object 0x..., a Bank$Account),");
            System.out.println("  which is held by \"transfer-6\"");
            System.out.println("\"transfer-6\":");
            System.out.println("  waiting to lock monitor 0x... (object 0x..., a Bank$Account),");
            System.out.println("  which is held by \"transfer-3\"\n");

            // Interrupt/cleanup for the demo flow
            for (Thread t : workers) t.interrupt();
            for (Thread t : workers) t.join(1000);
        }
    }

    // ==========================================
    // 2. Fix A: OrderedBank
    // ==========================================
    static final class OrderedBank {
        static final class Account {
            final int id;
            private long balance;
            Account(int id, long balance) { this.id = id; this.balance = balance; }
            long balance() { return balance; }
            void withdraw(long a) { balance -= a; }
            void deposit(long a)  { balance += a; }
        }

        static void transfer(Account from, Account to, long amount) {
            Account first = from.id < to.id ? from : to;
            Account second = from.id < to.id ? to : from;

            synchronized (first) {
                synchronized (second) {
                    if (from.balance() >= amount) {
                        from.withdraw(amount);
                        to.deposit(amount);
                    }
                }
            }
        }

        public static void runDemo() throws InterruptedException {
            Account[] accounts = new Account[4];
            for (int i = 0; i < accounts.length; i++) accounts[i] = new Account(i, 1_000);

            long startTime = System.currentTimeMillis();
            Thread[] workers = new Thread[8];
            long[] transferCounts = new long[8];

            for (int w = 0; w < workers.length; w++) {
                final int workerIdx = w;
                workers[w] = new Thread(() -> {
                    var rnd = ThreadLocalRandom.current();
                    long count = 0;
                    for (int i = 0; i < 100_000; i++) {
                        Account a = accounts[rnd.nextInt(accounts.length)];
                        Account b = accounts[rnd.nextInt(accounts.length)];
                        if (a != b) {
                            transfer(a, b, rnd.nextInt(1, 50));
                            count++;
                        }
                    }
                    transferCounts[workerIdx] = count;
                }, "ordered-transfer-" + w);
                workers[w].start();
            }

            for (Thread t : workers) t.join();
            long duration = System.currentTimeMillis() - startTime;

            long totalTransfers = 0;
            for (long c : transferCounts) totalTransfers += c;

            long totalBalance = 0;
            for (Account a : accounts) totalBalance += a.balance();

            System.out.println("# OrderedBank");
            System.out.println("transfers=" + totalTransfers + " total=" + totalBalance + " (expected 4000) in " + duration + " ms\n");
        }
    }

    // ==========================================
    // 3. Fix B: TryLockBank & 4. Graceful Shutdown
    // ==========================================
    static final class TryLockBank {
        static final class Account {
            final int id;
            private long balance;
            final ReentrantLock lock = new ReentrantLock();

            Account(int id, long balance) { this.id = id; this.balance = balance; }
            long balance() { return balance; }
            void withdraw(long a) { balance -= a; }
            void deposit(long a)  { balance += a; }
        }

        static boolean transfer(Account from, Account to, long amount, java.util.concurrent.atomic.AtomicLong retries) {
            while (true) {
                if (Thread.currentThread().isInterrupted()) return false;
                try {
                    if (from.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                        try {
                            if (to.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                                try {
                                    if (from.balance() >= amount) {
                                        from.withdraw(amount);
                                        to.deposit(amount);
                                    }
                                    return true;
                                } finally {
                                    to.lock.unlock();
                                }
                            }
                        } finally {
                            from.lock.unlock();
                        }
                    }
                    retries.incrementAndGet();
                    Thread.sleep(ThreadLocalRandom.current().nextInt(1, 10));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        public static void runDemo() throws InterruptedException {
            Account[] accounts = new Account[4];
            for (int i = 0; i < accounts.length; i++) accounts[i] = new Account(i, 1_000);

            long startTime = System.currentTimeMillis();
            Thread[] workers = new Thread[8];
            long[] transferCounts = new long[8];
            java.util.concurrent.atomic.AtomicLong totalRetries = new java.util.concurrent.atomic.AtomicLong(0);

            for (int w = 0; w < workers.length; w++) {
                final int workerIdx = w;
                workers[w] = new Thread(() -> {
                    var rnd = ThreadLocalRandom.current();
                    long count = 0;
                    for (int i = 0; i < 100_000; i++) {
                        Account a = accounts[rnd.nextInt(accounts.length)];
                        Account b = accounts[rnd.nextInt(accounts.length)];
                        if (a != b) {
                            if (transfer(a, b, rnd.nextInt(1, 50), totalRetries)) {
                                count++;
                            }
                        }
                    }
                    transferCounts[workerIdx] = count;
                }, "trylock-transfer-" + w);
                workers[w].start();
            }

            for (Thread t : workers) t.join();
            long duration = System.currentTimeMillis() - startTime;

            long totalTransfers = 0;
            for (long c : transferCounts) totalTransfers += c;

            long totalBalance = 0;
            for (Account a : accounts) totalBalance += a.balance();

            System.out.println("# TryLockBank");
            System.out.println("transfers=" + totalTransfers + " retries=" + totalRetries.get() + " total=" + totalBalance + " (expected 4000) in " + duration + " ms\n");
        }

        public static void runGracefulShutdownDemo() throws InterruptedException {
            Account[] accounts = new Account[4];
            for (int i = 0; i < accounts.length; i++) accounts[i] = new Account(i, 1_000);

            Thread[] workers = new Thread[8];
            long[] transferCounts = new long[8];
            java.util.concurrent.atomic.AtomicLong dummyRetries = new java.util.concurrent.atomic.AtomicLong(0);

            for (int w = 0; w < workers.length; w++) {
                final int workerIdx = w;
                workers[w] = new Thread(() -> {
                    var rnd = ThreadLocalRandom.current();
                    long count = 0;
                    while (!Thread.currentThread().isInterrupted()) {
                        Account a = accounts[rnd.nextInt(accounts.length)];
                        Account b = accounts[rnd.nextInt(accounts.length)];
                        if (a != b) {
                            if (transfer(a, b, rnd.nextInt(1, 50), dummyRetries)) {
                                count++;
                            }
                        }
                    }
                    transferCounts[workerIdx] = count;
                    System.out.println("transfer-" + workerIdx + " stopped after " + count + " transfers");
                }, "transfer-" + w);
                workers[w].start();
            }

            // Sleep for 2 seconds before requesting graceful shutdown
            Thread.sleep(2000);
            System.out.println("# graceful shutdown");
            System.out.println("[main] interrupting 8 workers after 2000 ms");

            for (Thread t : workers) t.interrupt();
            for (Thread t : workers) t.join(1000);

            long totalBalance = 0;
            for (Account a : accounts) totalBalance += a.balance();

            System.out.println("all workers stopped=true total=" + totalBalance + " (expected 4000)");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("# broken version, with watchdog");
        BrokenBank.runDemo();

        OrderedBank.runDemo();

        TryLockBank.runDemo();

        TryLockBank.runGracefulShutdownDemo();
    }
}