package com.poc.meta;

import java.util.List;
import java.util.concurrent.Callable;

public class OLTPWorker implements Callable<OLTPWorker.Result> {

    private final ProxyClient proxy;
    private final int txCount;
    private final List<String> oltpQueries;

    public static class Result {
        public final int transactions;
        public final long totalNanos;

        public Result(int transactions, long totalNanos) {
            this.transactions = transactions;
            this.totalNanos = totalNanos;
        }
    }

    public OLTPWorker(ProxyClient proxy, int txCount, List<String> oltpQueries) {
        this.proxy = proxy;
        this.txCount = txCount;
        this.oltpQueries = oltpQueries;
    }

    @Override
    public Result call() throws Exception {
        long start = System.nanoTime();
        int done = 0;

        for (int i = 0; i < txCount; i++) {
            // exécuter toutes les requêtes OLTP, ou les parcourir en round‑robin
            for (String sql : oltpQueries) {
                proxy.executeUpdate(sql);
            }
            done++;
        }

        long end = System.nanoTime();
        return new Result(done, end - start);
    }
}
