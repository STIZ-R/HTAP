package com.htap.meta;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadUtils {
    public static ExecutorService createFixedThreadPool(int nThreads) {
        return Executors.newFixedThreadPool(nThreads);
    }
}
