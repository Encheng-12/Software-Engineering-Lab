package edu.itc.se.lab01.task3;

import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        OrderQueue buffer = new BoundedBuffer(5);
        AtomicInteger processedCount = new AtomicInteger(0);

        // 3 consumers, so we need 3 poison pills total distributed among producers (e.g., 2 and 1)
        Thread p1 = new Thread(new Producer(buffer, 50, 2), "producer-1");
        Thread p2 = new Thread(new Producer(buffer, 50, 1), "producer-2");

        Thread c1 = new Thread(new Consumer("consumer-1", buffer, processedCount));
        Thread c2 = new Thread(new Consumer("consumer-2", buffer, processedCount));
        Thread c3 = new Thread(new Consumer("consumer-3", buffer, processedCount));

        p1.start();
        p2.start();
        c1.start();
        c2.start();
        c3.start();

        p1.join();
        p2.join();
        c1.join();
        c2.join();
        c3.join();

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Total processed: " + processedCount.get() + " (expected 100) in " + duration + " ms");
        
        assert processedCount.get() == 100 : "Assertion failed: Total processed is not 100!";
    }
}