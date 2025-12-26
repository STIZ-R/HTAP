package com.poc.meta;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enregistre les métriques dans un CSV simple.
 * Format : ts_client,phase,type,thread_id,query_name,latency_ms,freshness_ms,extra
 */
public class MetricsRecorder implements AutoCloseable {

    private final PrintWriter writer;
    private final AtomicBoolean headerWritten = new AtomicBoolean(false);

    public MetricsRecorder(String filePath) throws IOException {
        this.writer = new PrintWriter(new FileWriter(filePath, true), true);
    }

    private void writeHeaderIfNeeded() {
        if (headerWritten.compareAndSet(false, true)) {
            writer.println("ts_client,phase,type,thread_id,query_name,latency_ms,freshness_ms,extra");
        }
    }

    public synchronized void record(
            String phase,
            String type,
            int threadId,
            String queryName,
            long latencyMs,
            Long freshnessMs,
            String extra
    ) {
        writeHeaderIfNeeded();
        long ts = System.currentTimeMillis();
        writer.printf("%d,%s,%s,%d,%s,%d,%s,%s%n",
                ts,
                phase,
                type,
                threadId,
                queryName,
                latencyMs,
                (freshnessMs == null ? "" : freshnessMs.toString()),
                (extra == null ? "" : extra)
        );
    }

    @Override
    public void close() {
        writer.flush();
        writer.close();
    }
}
