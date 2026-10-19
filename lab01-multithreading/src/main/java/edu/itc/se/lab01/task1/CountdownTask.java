package edu.itc.se.lab01.task1;

import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.TimeUnit;

public class CountdownTask implements Runnable {

    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        for (int i = 3; i >= 1; i--) {
            System.out.println(name + ": " + i);
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(200));
        }
        System.out.println(name + ": liftoff!");
    }
}