package com.poc.meta;

import java.util.*;
import java.util.concurrent.*;

/*
 * Runner de benchmark HTAP via HTTP proxy uniquement.
 *
 * Simule des requêtes OLTP et OLAP envoyées au proxy HTAP,
 * mesure le TPS global.
 */
public class HTAPRunner {

    public static void main(String[] args) throws Exception {
        /* URL du proxy */
        String proxyUrl = System.getenv().getOrDefault(
                "PROXY_URL",
                "http://localhost:8080/proxy/query"
        );
        System.out.println("Using PROXY_URL=" + proxyUrl);

        ProxyClient proxyClient = new ProxyClient(proxyUrl);

        /* Attente que le proxy soit prêt */
        waitForProxy(proxyClient);

        /* Paramètres du benchmark */
        int oltpThreads = 8;
        int txPerThread = 1000;
        int batchSize = 50;
        int olapThreads = 4;

        ExecutorService pool = Executors.newFixedThreadPool(oltpThreads + olapThreads);

        /* Lancer les threads OLAP */
        for (int i = 0; i < olapThreads; i++) {
            pool.submit(new OLAPWorker(proxyClient));
        }

        /* Lancer les threads OLTP et stocker les futures */
        List<Future<OLTPWorker.Result>> oltpFutures = new ArrayList<>();
        for (int i = 0; i < oltpThreads; i++) {
            oltpFutures.add(pool.submit(new OLTPWorker(proxyClient, txPerThread, batchSize)));
        }

        /* Collecte des résultats OLTP */
        int totalTx = 0;
        long maxNanos = 0;
        for (Future<OLTPWorker.Result> f : oltpFutures) {
            OLTPWorker.Result r = f.get();
            totalTx += r.transactions;
            maxNanos = Math.max(maxNanos, r.totalNanos);
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.MINUTES);

        /* Calcul du TPS */
        double seconds = maxNanos / 1_000_000_000.0;
        double tps = totalTx / seconds;

        System.out.printf("[OLTP via proxy] totalTx=%d, time=%.2fs, TPS=%.2f%n",
                totalTx, seconds, tps);
    }

    private static void waitForProxy(ProxyClient proxyClient) throws InterruptedException {
        int maxAttempts = 60;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                proxyClient.executeSingle("SELECT 1");
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
