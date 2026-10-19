package edu.itc.se.lab01.task2;

import java.util.concurrent.locks.ReentrantLock;

// Step 5: LockCounter using ReentrantLock with unlock in finally
class LockCounter implements Counter {
    private long count = 0;
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long get() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
