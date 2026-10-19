package edu.itc.se.lab01.task3;

public final class BlockingOrderQueue implements OrderQueue {
    private final java.util.concurrent.BlockingQueue<Order> q;

    public BlockingOrderQueue(int capacity) {
        this.q = new java.util.concurrent.ArrayBlockingQueue<>(capacity);
    }

    @Override
    public void put(Order o) throws InterruptedException {
        q.put(o);
    }

    @Override
    public Order take() throws InterruptedException {
        return q.take();
    }
}