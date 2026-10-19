package edu.itc.se.lab01.task2;

import java.util.concurrent.atomic.LongAdder;

// Step 6 (Bonus): LongAdderCounter 
// Why is it faster under high contention? 
// Instead of a single volatile variable bottlenecked by CPU cache invalidations across threads,
// LongAdder maintains a cell array of variables that dynamically grow to reduce contention.
class LongAdderCounter implements Counter {
    private final LongAdder count = new LongAdder();

    @Override
    public void increment() {
        count.increment();
    }

    @Override
    public long get() {
        return count.sum();
    }
}