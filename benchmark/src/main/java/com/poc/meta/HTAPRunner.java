package com.poc.meta;

import java.util.*;
import java.util.concurrent.*;

/**
 * Runner de benchmark HTAP via HTTP proxy.
 *
 * - Phase 0 : attente proxy + warm-up CDC (Postgres -> ClickHouse)
 * - Phase 1 : lancement d'un thread de mesure de fraîcheur OLAP
 * - Phase 2 : exécution des threads OLTP + OLAP et mesure du TPS OLTP
 */
public class HTAPRunner {

    public static void main(String[] args) throws Exception {

        String proxyUrl = System.getenv().getOrDefault(
                "PROXY_URL",
                "http://localhost:8080/proxy/query"
        );
        System.out.println("Using PROXY_URL=" + proxyUrl);

        ProxyClient proxyClient = new ProxyClient(proxyUrl);

        waitForProxy(proxyClient);

        warmupCdc(proxyClient);

        startFreshnessMonitor(proxyClient);

        // Paramètres du benchmark
        int oltpThreads = 6;
        int txPerThread = 2000;     // nombre de commandes par thread
        int batchSize = 700;        // taille du batch HTTP (nombre de statements)
        int olapThreads = 3;

        ExecutorService pool = Executors.newFixedThreadPool(oltpThreads + olapThreads);

        for (int i = 0; i < olapThreads; i++) {
            pool.submit(new OLAPWorker(proxyClient));
        }

        List<Future<OLTPWorker.Result>> oltpFutures = new ArrayList<>();
        for (int i = 0; i < oltpThreads; i++) {
            oltpFutures.add(pool.submit(new OLTPWorker(proxyClient, txPerThread, batchSize, i)));
        }

        int totalStatements = 0;
        long maxNanos = 0;
        for (Future<OLTPWorker.Result> f : oltpFutures) {
            OLTPWorker.Result r = f.get();
            totalStatements += r.statements;
            maxNanos = Math.max(maxNanos, r.totalNanos);
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.MINUTES);

        double seconds = maxNanos / 1_000_000_000.0;
        double tps = totalStatements / seconds;

        System.out.printf("[OLTP via proxy] totalStatements=%d, time=%.2fs, TPS=%.2f%n",
                totalStatements, seconds, tps);
    }

    /**
     * Attente active que le proxy réponde à une requête simple.
     */
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

    /**
     * Phase de warm-up CDC : envoie un INSERT marqueur côté OLTP
     * et boucle en OLAP jusqu'à ce que ClickHouse le voie.
     */
    private static void warmupCdc(ProxyClient proxyClient) throws Exception {
        System.out.println("Starting CDC warm-up...");

        String markerSql =
                "INSERT INTO start (id, debug) VALUES (1, 42) " +
                        "ON CONFLICT (id) DO UPDATE SET debug = 42";

        proxyClient.executeSingle(markerSql);

        long start = System.currentTimeMillis();
        long timeoutMs = 60_000;

        while (true) {
            try {
                int cnt = proxyClient.executeScalarInt(
                        "SELECT count(*) AS cnt " +
                                "FROM start " +
                                "WHERE id = 1 AND debug = 42"
                );
                if (cnt > 0) {
                    long elapsed = System.currentTimeMillis() - start;
                    System.out.printf("CDC warm-up done (start): marker visible in %d ms%n", elapsed);
                    return;
                }
            } catch (Exception ignored) {
            }

            if (System.currentTimeMillis() - start > timeoutMs) {
                throw new RuntimeException("ClickHouse n'a pas vu le marqueur start(id=1,debug=42) à temps");
            }
            Thread.sleep(500);
        }
    }


    /**
     * Thread de mesure de fraîcheur : calcule en continu la différence
     * entre 'now côté client' et le max(o_entry_d) visible côté OLAP.
     */
    private static void startFreshnessMonitor(ProxyClient proxyClient) {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    long tClient = System.currentTimeMillis();

                    Long lastTsMillis = proxyClient.executeScalarTimestampMillis(
                            "SELECT max(o_entry_d) AS last_ts " +
                                    "FROM orders"
                    );

                    if (lastTsMillis != null) {
                        long freshnessMs = tClient - lastTsMillis;
                        System.out.printf("[FRESHNESS] %d ms%n", freshnessMs);
                    } else {
                        System.out.println("[FRESHNESS] aucun ordre visible");
                    }

                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                System.err.println("Freshness monitor stopped: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }
}
