package com.poc.meta;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;

/**
 * Runner de benchmark HTAP via HTTP proxy.
 */
public class HTAPRunner {

    public static void main(String[] args) throws Exception {

        Properties props = loadBenchmarkProperties();

        String runMode = props.getProperty("run.mode", "htap").trim(); // oltp_only ou htap

        String proxyUrl = System.getenv().getOrDefault(
                "PROXY_URL",
                "http://localhost:8080/proxy/query"
        );
        System.out.println("Using PROXY_URL=" + proxyUrl);

        String metricsFile = System.getenv().getOrDefault(
                "METRICS_FILE",
                props.getProperty("metrics.file", "htap_metrics.csv")
        );
        System.out.println("Using METRICS_FILE=" + metricsFile);

        int oltpThreads = Integer.parseInt(props.getProperty("oltp.threads", "7"));
        int txPerThread = Integer.parseInt(props.getProperty("oltp.txPerThread", "2000"));
        int batchSize   = Integer.parseInt(props.getProperty("oltp.batchSize", "800"));
        int olapThreads = Integer.parseInt(props.getProperty("olap.threads", "3"));

        String oltpWorkloadPath = props.getProperty("workload.oltp", "workload/oltp.sql");
        String olapWorkloadPath = props.getProperty("workload.olap", "workload/olap.sql");

        List<String> oltpTemplates = WorkloadLoader.loadSqlFile(oltpWorkloadPath);
        List<String> olapQueries   = WorkloadLoader.loadSqlFile(olapWorkloadPath);

        ProxyClient proxyClient = new ProxyClient(proxyUrl);

        waitForProxy(proxyClient);

        warmupCdc(proxyClient);

        try (MetricsRecorder recorder = new MetricsRecorder(metricsFile)) {

            if (!"oltp_only".equalsIgnoreCase(runMode)) {
                startFreshnessMonitor(proxyClient, recorder);
            }

            ExecutorService pool;
            if ("oltp_only".equalsIgnoreCase(runMode)) {
                olapThreads = 0;
                pool = Executors.newFixedThreadPool(oltpThreads);
            } else {
                pool = Executors.newFixedThreadPool(oltpThreads + olapThreads);
            }

            for (int i = 0; i < olapThreads; i++) {
                pool.submit(new OLAPWorker(proxyClient, olapQueries, recorder, i));
            }

            List<Future<OLTPWorker.Result>> oltpFutures = new ArrayList<>();
            for (int i = 0; i < oltpThreads; i++) {
                oltpFutures.add(pool.submit(
                        new OLTPWorker(proxyClient, oltpTemplates, txPerThread, batchSize, i, recorder, runMode)
                ));
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

            System.out.printf("[OLTP via proxy][mode=%s] totalStatements=%d, time=%.2fs, TPS=%.2f%n",
                    runMode, totalStatements, seconds, tps);
        }
    }

    private static Properties loadBenchmarkProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream is = HTAPRunner.class.getClassLoader()
                .getResourceAsStream("benchmark.properties")) {
            if (is != null) {
                props.load(is);
            }
        }
        return props;
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
            } catch (Exception ignored) {}

            if (System.currentTimeMillis() - start > timeoutMs) {
                throw new RuntimeException("ClickHouse n'a pas vu le marqueur start(id=1,debug=42) à temps");
            }
            Thread.sleep(500);
        }
    }

    private static void startFreshnessMonitor(ProxyClient proxyClient, MetricsRecorder recorder) {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    long tClient = System.currentTimeMillis();

                    Long lastTsMillis = proxyClient.executeScalarTimestampMillis(
                            "SELECT max(o_entry_d) AS last_ts FROM orders"
                    );

                    Long freshnessMs = null;
                    if (lastTsMillis != null) {
                        freshnessMs = tClient - lastTsMillis;
                        System.out.printf("[FRESHNESS_MONITOR] %d ms%n", freshnessMs);
                    } else {
                        System.out.println("[FRESHNESS_MONITOR] aucun ordre visible");
                    }

                    recorder.record("monitor", "OLAP_FRESHNESS", -1,
                            "MONITOR_QUERY", 0L, freshnessMs, null);

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
