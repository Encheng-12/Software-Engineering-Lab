package edu.itc.se.lab01.task4;

import java.io.IOException;
import java.util.concurrent.Callable;

public class FakeDownload implements Callable<String> {
    private final String url;
    private final int index;

    public FakeDownload(String url, int index) {
        this.url = url;
        this.index = index;
    }

    @Override
    public String call() throws Exception {
        // Every 50th task throws an IOException ("boom")[cite: 5]
        if (index > 0 && index % 50 == 0) {
            throw new IOException("boom");
        }

        // Simulate network delay or the slow 5-second task[cite: 5, 7]
        if ("http://slow".equals(url)) {
            Thread.sleep(5000);
        } else {
            Thread.sleep(200);
        }

        return url + " -> " + url.length();
    }
}