package com.poc.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

public class OLTPWorker implements Callable<OLTPWorker.Result> {

    private final ProxyClientInterface client;
    private final List<String> templates;
    private final int totalTx;
    private final int batchSize;
    private final int workerId;
    private final MetricsRecorder recorder;
    private final String phase;
    private final int nbWarehouses;
    private final int nbDistricts;

    public OLTPWorker(ProxyClientInterface client,
                      List<String> templates,
                      int totalTx,
                      int batchSize,
                      int workerId,
                      MetricsRecorder recorder,
                      String phase,
                      int nbWarehouses,
                      int nbDistricts) {
        this.client       = client;
        this.templates    = templates;
        this.totalTx      = totalTx;
        this.batchSize    = batchSize;
        this.workerId     = workerId;
        this.recorder     = recorder;
        this.phase        = phase;
        this.nbWarehouses = nbWarehouses;
        this.nbDistricts  = nbDistricts;
    }

    @Override
    public Result call() throws Exception {

        ThreadLocalRandom rng = ThreadLocalRandom.current();

        List<String> batch = new ArrayList<>(batchSize);
        int doneStatements = 0;
        long startNanos = System.nanoTime();

        String orderTemplate = templates.get(0);
        String lineTemplate  = templates.get(1);

        for (int i = 0; i < totalTx; i++) {

            int wId   = 1 + (workerId % nbWarehouses);
            int dId   = 1 + (i % nbDistricts);
            int cId   = 1 + rng.nextInt(100000);
            int oId   = workerId * 1_000_000 + i;
            int olCnt = 5;

            String orderSql = orderTemplate
                    .replace("$W_ID$",      String.valueOf(wId))
                    .replace("$D_ID$",      String.valueOf(dId))
                    .replace("$O_ID$",      String.valueOf(oId))
                    .replace("$C_ID$",      String.valueOf(cId))
                    .replace("$OL_CNT$",    String.valueOf(olCnt))
                    .replace("$ALL_LOCAL$", "1");
            batch.add(orderSql);

            for (int line = 1; line <= olCnt; line++) {
                int    itemId = 1 + rng.nextInt(100000);
                int    qty    = 1 + rng.nextInt(10);
                double amount = qty * (1.0 + rng.nextDouble() * 100.0);
                String distInfo = String.format("dist_%02d_%016d", dId, (long)(rng.nextDouble() * 1e16));

                String olSql = lineTemplate
                        .replace("$W_ID$",      String.valueOf(wId))
                        .replace("$D_ID$",      String.valueOf(dId))
                        .replace("$O_ID$",      String.valueOf(oId))
                        .replace("$OL_NO$",     String.valueOf(line))
                        .replace("$ITEM_ID$",   String.valueOf(itemId))
                        .replace("$QTY$",       String.valueOf(qty))
                        .replace("$AMOUNT$",    String.format(Locale.US, "%.2f", amount))
                        .replace("$DIST_INFO$", "'" + distInfo + "'");
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

        // Flush du batch résiduel
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

    // -------------------------------------------------------

    public static class Result {
        public final int  statements;
        public final long totalNanos;

        public Result(int statements, long totalNanos) {
            this.statements = statements;
            this.totalNanos = totalNanos;
        }

        /** TPS calculé à partir du nombre de statements envoyés. */
        public double tps() {
            return statements / (totalNanos / 1_000_000_000.0);
        }
    }
}
