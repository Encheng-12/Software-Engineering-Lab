package edu.itc.se.lab01.task3;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.TimeUnit;

public class Producer implements Runnable {
    private final OrderQueue queue;
    private final int ordersCount;
    private final int poisonPillsCount;

    public Producer(OrderQueue queue, int ordersCount, int poisonPillsCount) {
        this.queue = queue;
        this.ordersCount = ordersCount;
        this.poisonPillsCount = poisonPillsCount;
    }

    @Override
    public void run() {
        try {
            Random random = new Random();
            for (int i = 1; i <= ordersCount; i++) {
                Order order = new Order(i, "item-" + i, random.nextInt(5) + 1);
                queue.put(order);
                int millis = ThreadLocalRandom.current().nextInt(0, 21);
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(millis));
            }

            for (int p = 0; p < poisonPillsCount; p++) {
                queue.put(new Order(-1, "POISON_PILL", 0));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}