package edu.itc.se.lab01.task3;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.TimeUnit;

public class Consumer implements Runnable {
    private final OrderQueue queue;
    private final AtomicInteger processedCount;
    private final String name;

    public Consumer(String name, OrderQueue queue, AtomicInteger processedCount) {
        this.name = name;
        this.queue = queue;
        this.processedCount = processedCount;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Order order = queue.take();
                if (order.id() == -1) {
                    System.out.println(name + " received poison pill, stopping");
                    break;
                }
                
                int millis = ThreadLocalRandom.current().nextInt(5, 31);
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(millis));
                
                processedCount.incrementAndGet();
                System.out.println(name + " processed " + order);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}