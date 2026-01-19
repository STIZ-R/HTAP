package com.poc.meta;

import java.util.List;

/**
 * Worker OLAP :
 * - Exécute une requête analytique depuis workload/olap.sql.
 * - Calcule latence + freshness par requête.
 */
public class OLAPWorker implements Runnable {

    private final ProxyClientInterface client;
    private final List<String> olapQueries;
    private final MetricsRecorder recorder;
    private final int workerId;

    public OLAPWorker(ProxyClientInterface client,
                      List<String> olapQueries,
                      MetricsRecorder recorder,
                      int workerId) {
        this.client = client;
        this.olapQueries = olapQueries;
        this.recorder = recorder;
        this.workerId = workerId;
    }

    @Override
    public void run() {
        // On prend la première requête comme OLAP principale.
        String mainOlapQuery = olapQueries.get(0);
        String freshnessProbe = (olapQueries.size() > 1)
                ? olapQueries.get(1)
                : "SELECT max(o_entry_d) AS last_ts FROM orders";

        try {
            while (true) {
                long tClientStart = System.currentTimeMillis();

                // Exécution de la requête OLAP principale (on ignore le contenu)
                client.executeSingle(mainOlapQuery);
                long latency = System.currentTimeMillis() - tClientStart;

                // Freshness liée à cette requête : max(o_entry_d) visible
                Long lastTsMillis = client.executeScalarTimestampMillis(freshnessProbe);
                Long freshnessMs = null;
                if (lastTsMillis != null) {
                    freshnessMs = tClientStart - lastTsMillis;
                }

                recorder.record("htap", "OLAP_QUERY", workerId,
                        "MAIN_OLAP", latency, freshnessMs, null);

                Thread.sleep(500);
            }
        } catch (Exception ignored) {
        }
    }
}
