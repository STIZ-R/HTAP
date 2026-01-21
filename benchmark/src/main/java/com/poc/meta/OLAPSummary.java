package com.poc.meta;

import java.nio.file.Path;
import java.util.List;

public class OLAPSummary {

    public static void runOnFile(String filePath) throws Exception {
        List<CsvMetricsAnalyzer.Record> records =
                CsvMetricsAnalyzer.read(Path.of(filePath));

        long firstTs = Long.MAX_VALUE;
        long lastTs  = 0;

        int count = 0;
        long sumLatency = 0;

        // pour percentile simple
        java.util.List<Long> latencies = new java.util.ArrayList<>();

        for (CsvMetricsAnalyzer.Record r : records) {
            if (!"OLAP_QUERY".equals(r.type)) continue;

            count++;
            sumLatency += r.latencyMs;
            latencies.add(r.latencyMs);

            firstTs = Math.min(firstTs, r.tsClient);
            lastTs  = Math.max(lastTs, r.tsClient);
        }

        if (count == 0) {
            System.out.println("[SUMMARY][OLAP] aucune requête OLAP trouvée");
            return;
        }

        double durationSeconds = (lastTs - firstTs) / 1000.0;
        double qph = count * 3600.0 / durationSeconds;
        double avgLatency = sumLatency / (double) count;

        latencies.sort(Long::compareTo);
        long p95 = latencies.get((int)(latencies.size() * 0.95));

        System.out.println("========== OLAP SUMMARY ==========");
        System.out.printf("Requêtes OLAP totales : %d%n", count);
        System.out.printf("Durée effective       : %.2f s%n", durationSeconds);
        System.out.printf("QpH (queries/hour)   : %.2f%n", qph);
        System.out.printf("Latence moyenne      : %.2f ms%n", avgLatency);
        System.out.printf("Latence p95          : %d ms%n", p95);
        System.out.println("=================================");
    }
}
