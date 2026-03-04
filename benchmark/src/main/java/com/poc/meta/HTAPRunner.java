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




        String metricsFile = System.getenv().getOrDefault(
                "METRICS_FILE",
                props.getProperty("metrics.file", "htap_metrics.csv")
        );
        System.out.println("Using METRICS_FILE=" + metricsFile);

        String clientMode = props.getProperty("client.mode", "jdbc").trim().toLowerCase();
        System.out.println("Using client.mode=" + clientMode);


        int oltpThreads = Integer.parseInt(props.getProperty("oltp.threads", "7"));
        int txPerThread = Integer.parseInt(props.getProperty("oltp.txPerThread", "2000"));
        int batchSize   = Integer.parseInt(props.getProperty("oltp.batchSize", "800"));
        int olapThreads = Integer.parseInt(props.getProperty("olap.threads", "3"));

        long durationSeconds = Long.parseLong(props.getProperty("run.durationSeconds", "300"));
        long endTime = System.currentTimeMillis() + durationSeconds * 1000L;

        String oltpWorkloadPath = props.getProperty("workload.oltp", "workload/oltp.sql");
        String olapWorkloadPath = props.getProperty("workload.olap", "workload/olap.sql");
        String populatePath = props.getProperty("populate.oltp", "workload/populate.sql");
        String olapOnlyPath = props.getProperty("workload.olap_only", "workload/olap_only.sql");

        int nbWarehouses = Integer.parseInt(props.getProperty("oltp.warehouses", "1"));
        int nbDistricts  = Integer.parseInt(props.getProperty("oltp.districts",  "1"));

        boolean oltpOnly = "oltp_only".equalsIgnoreCase(runMode);
        boolean htapMode = "htap".equalsIgnoreCase(runMode);
        boolean olapOnly = "olap_only".equalsIgnoreCase(runMode);

        System.out.println("====================================");
        System.out.println(" HTAP Benchmark Runner");
        System.out.println(" run.mode   = " + runMode);
        System.out.println(" client.mode= " + clientMode);
        System.out.println("====================================");

        List<String> oltpTemplates = Collections.emptyList();
        List<String> olapQueries   = Collections.emptyList();
        List<String> populateSql   = Collections.emptyList();

        if (!olapOnly) {
            oltpTemplates = WorkloadLoader.loadSqlFile(oltpWorkloadPath);
            olapQueries   = WorkloadLoader.loadSqlFile(olapWorkloadPath);
        } else {
            // OLAP_ONLY : on charge populate + requêtes OLAP_ONLY
            if (populatePath == null || olapOnlyPath == null) {
                throw new IllegalArgumentException("populate.oltp et workload.olap_only doivent être définis en olap_only");
            }
            //populateSql = WorkloadLoader.loadSqlFile(populatePath);
            olapQueries = WorkloadLoader.loadSqlFile(olapOnlyPath);
        }


        ProxyClientInterface proxyClient;

        if ("api".equals(clientMode)) {

            String proxyUrl = System.getenv().getOrDefault(
                    "PROXY_URL",
                    "http://htap-proxy:8080"
            );
            System.out.println("Using PROXY_URL=" + proxyUrl);

            proxyClient = new ProxyClient(proxyUrl);

        } else { // default = jdbc

            String jdbcUrl = System.getenv().getOrDefault(
                    "JDBC_URL",
                    "jdbc:htap:http://htap-proxy:8080"
            );
            System.out.println("Using JDBC_URL=" + jdbcUrl);

            proxyClient = new JdbcProxyClient(jdbcUrl);
        }



        waitForProxy(proxyClient);
        warmupCdc(proxyClient);

        // =======================
        // MODE OLAP_ONLY
        // =======================
        if ("olap_only".equalsIgnoreCase(runMode)) {

            System.out.println("=== OLAP_ONLY MODE : populating OLTP first ===");

            // 1. Charger le fichier populate.sql
            List<String> populateStatements = WorkloadLoader.loadSqlFile(populatePath);

            System.out.println("Populate statements loaded: " + populateStatements.size());

            // 2. Envoyer en batch (important !)
            int batchSizePopulate = 1000;
            List<String> batch = new ArrayList<>();

            for (String sql : populateStatements) {
                batch.add(sql);
                if (batch.size() >= batchSizePopulate) {
                    proxyClient.executeBatch(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                proxyClient.executeBatch(batch);
            }

            System.out.println("Populate finished, waiting for CDC to catch up...");

            // 3. Attendre que ClickHouse voie des données (barrière simple)
            waitUntilOlapHasData(proxyClient);

            // 4. OLAP ONLY → pas de threads OLTP
            oltpThreads = 0;

            // 5. Charger les requêtes OLAP_ONLY
            olapQueries = WorkloadLoader.loadSqlFile(olapOnlyPath);

            System.out.println("OLAP_ONLY queries loaded: " + olapQueries.size());
        }



        try (MetricsRecorder recorder = new MetricsRecorder(metricsFile)) {

            if (htapMode) {
                startFreshnessMonitor(proxyClient, recorder);
            }

            // En oltp_only → pas d’OLAP
            if (oltpOnly) {
                olapThreads = 0;
            }

            // En olap_only → pas d’OLTP
            if (olapOnly) {
                oltpThreads = 0;
            }


            ExecutorService pool = Executors.newFixedThreadPool(oltpThreads + olapThreads);

            for (int i = 0; i < olapThreads; i++) {
                pool.submit(new OLAPWorker(proxyClient, olapQueries, recorder, i));
            }

            List<Future<OLTPWorker.Result>> oltpFutures = new ArrayList<>();
            for (int i = 0; i < oltpThreads; i++) {
                oltpFutures.add(pool.submit(
                        new OLTPWorker(proxyClient, oltpTemplates, txPerThread, batchSize, i, recorder, runMode, nbWarehouses, nbDistricts)
                ));
            }

            while (System.currentTimeMillis() < endTime) {
                Thread.sleep(1000);
            }

            pool.shutdown();
            pool.awaitTermination(30, TimeUnit.SECONDS);


            if (!olapOnly) {
                int totalStatements = 0;
                long maxNanos = 0;
                for (Future<OLTPWorker.Result> f : oltpFutures) {
                    try {
                        OLTPWorker.Result r = f.get(30, TimeUnit.SECONDS);
                        totalStatements += r.statements;
                        maxNanos = Math.max(maxNanos, r.totalNanos);
                    } catch (TimeoutException te) {
                        System.err.println("Timeout en attendant un OLTPWorker, on continue sans lui.");
                    } catch (InterruptedException ie) {
                        System.err.println("Main thread interrompu pendant get(), on sort.");
                        Thread.currentThread().interrupt();
                        break;
                    } catch (ExecutionException ee) {
                        System.err.println("OLTPWorker a échoué: " + ee.getCause());
                    }
                }

                if (maxNanos > 0) {
                    double seconds = maxNanos / 1_000_000_000.0;
                    double tps = totalStatements / seconds;

                    System.out.printf("[OLTP via proxy][mode=%s] totalStatements=%d, time=%.2fs, TPS=%.2f%n",
                            runMode, totalStatements, seconds, tps);
                } else {
                    System.out.println("[OLTP via proxy][mode=" + runMode + "] TPS non calculable (threads interrompus ou erreurs)");
                }

                try {
                    MetricsSummary.runOnFile(metricsFile);
                    if ("olap_only".equalsIgnoreCase(runMode)) {
                        OLAPSummary.runOnFile(metricsFile);
                    }

                } catch (Exception e) {
                    System.err.println("Failed to run MetricsSummary: " + e.getMessage());
                }
            }
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

    private static void waitForProxy(ProxyClientInterface proxyClient) throws InterruptedException {
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


    private static void warmupCdc(ProxyClientInterface proxyClient) throws Exception {
        System.out.println("Starting CDC warm-up...");

        String markerSql =
                "INSERT INTO start (id, debug) VALUES (1, 42) " +
                        "ON CONFLICT (id) DO UPDATE SET debug = 42";

        proxyClient.executeSingle(markerSql);

        long start = System.currentTimeMillis();
        long timeoutMs = 600_000;

        while (true) {
            try {
                int cnt = proxyClient.executeScalarInt(
                        "SELECT count(*) AS cnt FROM START WHERE id=1 AND debug=42" +
                                " AND _deleted=0"  // ← Ignore soft-deletes sink
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

    private static void waitUntilOlapHasData(ProxyClientInterface proxyClient) throws Exception {
        long start = System.currentTimeMillis();
        long timeoutMs = 600_000; // 10 minutes max

        while (true) {
            try {
                int cnt = proxyClient.executeScalarInt(
                        "SELECT count(*) FROM OORDER WHERE _deleted=0"
                );

                if (cnt > 1000) {   // seuil minimal
                    long elapsed = System.currentTimeMillis() - start;
                    System.out.printf("CDC ready: %d orders visible after %d ms%n", cnt, elapsed);
                    return;
                }
            } catch (Exception ignored) {}

            if (System.currentTimeMillis() - start > timeoutMs) {
                throw new RuntimeException("CDC n'a pas répliqué orders à temps");
            }

            Thread.sleep(1000);
        }
    }


    private static void startFreshnessMonitor(ProxyClientInterface proxyClient, MetricsRecorder recorder) {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    long tClient = System.currentTimeMillis();

                    Long lastTsMillis = proxyClient.executeScalarTimestampMillis(
                            "SELECT max(o_entry_d) AS last_ts FROM OORDER"
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
