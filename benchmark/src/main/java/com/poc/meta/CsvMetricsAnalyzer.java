package com.poc.meta;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CsvMetricsAnalyzer {

    public static class Record {
        public long tsClient;
        public String phase;
        public String type;
        public int threadId;
        public String queryName;
        public long latencyMs;
        public Long freshnessMs;
        public String extra;
    }

    public static List<Record> read(Path file) throws Exception {
        List<Record> res = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file.toFile()))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { // sauter l'en-tête "ts_client,phase,..."
                    first = false;
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }

                String[] parts = line.split(",", 8); // 8 colonnes
                if (parts.length < 7) continue;

                Record r = new Record();
                r.tsClient   = Long.parseLong(parts[0]);
                r.phase      = parts[1];
                r.type       = parts[2];
                r.threadId   = Integer.parseInt(parts[3]);
                r.queryName  = parts[4];
                r.latencyMs  = Long.parseLong(parts[5]);
                r.freshnessMs = (parts[6].isEmpty() ? null : Long.parseLong(parts[6]));
                r.extra      = (parts.length >= 8 ? parts[7] : "");
                res.add(r);
            }
        }
        return res;
    }

}
