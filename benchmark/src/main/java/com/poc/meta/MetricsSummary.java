package com.poc.meta;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MetricsSummary {

    public static void main(String[] args) throws Exception {
        Path csv = Path.of(args.length > 0 ? args[0] : "htap_metrics.csv");

        List<CsvMetricsAnalyzer.Record> records = CsvMetricsAnalyzer.read(csv);

        summarizeOltp(records);
        summarizeOlapLatency(records);
        summarizeFreshness(records);
    }

    private static void summarizeOltp(List<CsvMetricsAnalyzer.Record> records) {
        List<CsvMetricsAnalyzer.Record> oltp = records.stream()
                .filter(r -> "OLTP_BATCH".equals(r.type))
                .collect(Collectors.toList());

        if (oltp.isEmpty()) {
            System.out.println("No OLTP_BATCH records");
            return;
        }

        long tsMin = oltp.stream().mapToLong(r -> r.tsClient).min().orElse(0L);
        long tsMax = oltp.stream().mapToLong(r -> r.tsClient).max().orElse(0L);
        long durationMs = tsMax - tsMin;

        long totalStatements = oltp.stream()
                .mapToLong(r -> parseStatements(r.extra))
                .sum();

        double seconds = durationMs / 1000.0;
        double tps = totalStatements / seconds;

        System.out.printf("OLTP: totalStatements=%d, duration=%.2fs, TPS=%.2f%n",
                totalStatements, seconds, tps);
    }

    private static long parseStatements(String extra) {
        // extra: "statements=804"
        if (extra == null || extra.isEmpty()) return 0;
        String[] parts = extra.split("=");
        return (parts.length == 2) ? Long.parseLong(parts[1]) : 0;
    }

    private static void summarizeOlapLatency(List<CsvMetricsAnalyzer.Record> records) {
        List<Long> latencies = records.stream()
                .filter(r -> "OLAP_QUERY".equals(r.type))
                .map(r -> r.latencyMs)
                .sorted()
                .collect(Collectors.toList());

        if (latencies.isEmpty()) {
            System.out.println("No OLAP_QUERY records");
            return;
        }

        System.out.printf("OLAP latency: p50=%d ms, p95=%d ms, max=%d ms%n",
                percentile(latencies, 50),
                percentile(latencies, 95),
                latencies.get(latencies.size() - 1));
    }

    private static void summarizeFreshness(List<CsvMetricsAnalyzer.Record> records) {
        List<Long> fresh = records.stream()
                .filter(r -> r.freshnessMs != null)
                .filter(r -> "OLAP_QUERY".equals(r.type)) // freshness par requête
                .map(r -> r.freshnessMs)
                .sorted()
                .collect(Collectors.toList());

        if (fresh.isEmpty()) {
            System.out.println("No freshness values for OLAP_QUERY");
            return;
        }

        System.out.printf("Freshness (OLAP_QUERY): p50=%d ms, p95=%d ms, max=%d ms%n",
                percentile(fresh, 50),
                percentile(fresh, 95),
                fresh.get(fresh.size() - 1));
    }

    private static long percentile(List<Long> sortedValues, int p) {
        if (sortedValues.isEmpty()) return 0;
        int idx = (int) Math.ceil(p / 100.0 * sortedValues.size()) - 1;
        idx = Math.max(0, Math.min(idx, sortedValues.size() - 1));
        return sortedValues.get(idx);
    }
}
