package edu.itc.se.lab01.task1;

import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.TimeUnit;

public class Countdown extends Thread {

    public Countdown(String name) {
        super(name);
    }

    @Override
    public void run() {
        for (int i = 3; i >= 1; i--) {
            System.out.println(getName() + ": " + i);
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(200));
        }
        System.out.println(getName() + ": liftoff!");
    }
}