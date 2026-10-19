package edu.itc.se.lab01.task2;

// Step 5: SynchronizedCounter using synchronized methods
class SynchronizedCounter implements Counter {
    private long count = 0;

    @Override
    public synchronized void increment() {
        count++;
    }

    @Override
    public synchronized long get() {
        return count;
    }
}
