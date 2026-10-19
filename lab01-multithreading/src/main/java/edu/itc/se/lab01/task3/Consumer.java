package edu.itc.se.lab01.task3;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;

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
                // "Process" order with a 5–30 ms sleep
                Thread.sleep(ThreadLocalRandom.current().nextInt(5, 31));
                processedCount.incrementAndGet();
                System.out.println(name + " processed " + order);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}