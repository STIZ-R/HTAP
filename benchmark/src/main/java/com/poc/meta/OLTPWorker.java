package com.poc.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class OLTPWorker implements Callable<OLTPWorker.Result> {

    private final ProxyClientInterface client;
    private final List<String> templates;
    private final int totalTx;
    private final int batchSize;
    private final int workerId;
    private final MetricsRecorder recorder;
    private final String phase;

    public OLTPWorker(ProxyClientInterface client,
                      List<String> templates,
                      int totalTx,
                      int batchSize,
                      int workerId,
                      MetricsRecorder recorder,
                      String phase) {
        this.client = client;
        this.templates = templates;
        this.totalTx = totalTx;
        this.batchSize = batchSize;
        this.workerId = workerId;
        this.recorder = recorder;
        this.phase = phase;
    }

    @Override
    public Result call() throws Exception {

        List<String> batch = new ArrayList<>(batchSize);
        int doneStatements = 0;
        long startNanos = System.nanoTime();

        int wId = 1;
        int dId = 1;
        int cId = 1;

        String orderTemplate = templates.get(0);
        String lineTemplate  = templates.get(1);

        for (int i = 0; i < totalTx; i++) {

            int oId = workerId * 1_000_000 + i;
            int olCnt = 5;

            String orderSql = orderTemplate
                    .replace("$W_ID$", String.valueOf(wId))
                    .replace("$D_ID$", String.valueOf(dId))
                    .replace("$O_ID$", String.valueOf(oId))
                    .replace("$C_ID$", String.valueOf(cId))
                    .replace("$OL_CNT$", String.valueOf(olCnt));
            batch.add(orderSql);

            for (int line = 1; line <= olCnt; line++) {
                int itemId = 1 + (int) (Math.random() * 100000);
                int qty = 1 + (int) (Math.random() * 10);
                double amount = qty * (1 + Math.random() * 100);

                String olSql = lineTemplate
                        .replace("$W_ID$", String.valueOf(wId))
                        .replace("$D_ID$", String.valueOf(dId))
                        .replace("$O_ID$", String.valueOf(oId))
                        .replace("$OL_NO$", String.valueOf(line))
                        .replace("$ITEM_ID$", String.valueOf(itemId))
                        .replace("$QTY$", String.valueOf(qty))
                        .replace("$AMOUNT$", String.format(java.util.Locale.US, "%.2f", amount));

                batch.add(olSql);
            }

            if (batch.size() >= batchSize) {
                long t0 = System.currentTimeMillis();
                client.executeBatch(batch);
                long latency = System.currentTimeMillis() - t0;

                doneStatements += batch.size();
                recorder.record(phase, "OLTP_BATCH", workerId,
                        "BATCH", latency, null,
                        "statements=" + batch.size());

                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            long t0 = System.currentTimeMillis();
            client.executeBatch(batch);
            long latency = System.currentTimeMillis() - t0;

            doneStatements += batch.size();
            recorder.record(phase, "OLTP_BATCH", workerId,
                    "BATCH", latency, null,
                    "statements=" + batch.size());
        }

        long totalNanos = System.nanoTime() - startNanos;
        return new Result(doneStatements, totalNanos);
    }

    public static class Result {
        public final int statements;
        public final long totalNanos;

        public Result(int statements, long totalNanos) {
            this.statements = statements;
            this.totalNanos = totalNanos;
        }
    }
}
