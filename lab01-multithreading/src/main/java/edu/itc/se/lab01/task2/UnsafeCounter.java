package edu.itc.se.lab01.task2;

// Step 2: UnsafeCounter with a plain long count and race condition
class UnsafeCounter implements Counter {
    private long count = 0;

    @Override
    public void increment() {
        count++; // Not atomic: read, increment, write can interleave
    }

    @Override
    public long get() {
        return count;
    }
}