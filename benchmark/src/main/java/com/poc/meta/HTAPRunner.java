package com.poc.meta;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class HTAPRunner {

    public static void main(String[] args) throws Exception {
        String proxyUrl = System.getenv().getOrDefault(
                "PROXY_URL",
                "http://localhost:8080/proxy/query"
        );
        System.out.println("Using PROXY_URL=" + proxyUrl);
        ProxyClient proxyClient = new ProxyClient(proxyUrl);

        waitForProxy(proxyClient);

        int oltpThreads = 4;
        int txPerThread = 500;
        int olapThreads = 2;

        // Requêtes OLTP TPC‑C simplifiées (par ex. new‑order fixe pour debug)
        List<String> oltpQueries = Arrays.asList(
                "INSERT INTO users (name, email) VALUES ('user_1', 'user1@example.com') " +
                        "ON CONFLICT (email) DO NOTHING",
                "INSERT INTO orders (user_id, amount) VALUES (1, 42.50)",
                "INSERT INTO orders (user_id, amount) VALUES (1, 19.99)"
        );



        // Requêtes OLAP
        List<String> olapQueries = Arrays.asList(
                "SELECT user_id, COUNT(*) AS nb_orders, SUM(amount) AS total_amount " +
                        "FROM orders GROUP BY user_id ORDER BY total_amount DESC",
                "SELECT SUM(amount) AS total_revenue FROM orders",
                "SELECT user_id, SUM(amount) AS total_amount " +
                        "FROM orders GROUP BY user_id ORDER BY total_amount DESC LIMIT 5"
        );


        ExecutorService pool = Executors.newFixedThreadPool(oltpThreads + olapThreads);

        @SuppressWarnings("unchecked")
        Future<OLTPWorker.Result>[] oltpFutures = new Future[oltpThreads];
        for (int i = 0; i < oltpThreads; i++) {
            oltpFutures[i] = pool.submit(new OLTPWorker(proxyClient, txPerThread, oltpQueries));
        }

        for (int i = 0; i < olapThreads; i++) {
            pool.submit(new OLAPWorker(proxyClient, olapQueries));
        }

        int totalTx = 0;
        long maxNanos = 0;
        for (Future<OLTPWorker.Result> f : oltpFutures) {
            OLTPWorker.Result r = f.get();
            totalTx += r.transactions;
            maxNanos = Math.max(maxNanos, r.totalNanos);
        }

        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        double seconds = maxNanos / 1_000_000_000.0;
        double tps = totalTx / seconds;
        System.out.printf("[OLTP via proxy] totalTx=%d, time=%.2fs, TPS=%.2f%n",
                totalTx, seconds, tps);
    }

    private static void waitForProxy(ProxyClient proxyClient) throws InterruptedException {
        int maxAttempts = 30;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                proxyClient.executeSelect("SELECT 1");
                System.out.println("Proxy is up");
                return;
            } catch (Exception e) {
                System.out.println("Proxy not ready, retrying in 1s...");
                Thread.sleep(1000);
            }
        }
        throw new RuntimeException("Proxy did not become ready after " + maxAttempts + " seconds");
    }
}
