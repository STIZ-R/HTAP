package com.poc.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Worker OLTP
 *
 * - Génère des commandes TPC-C-like dans orders + order_line.
 * - Accumule les INSERTs en mémoire et les envoie par batch via ProxyClient.
 */
public class OLTPWorker implements Callable<OLTPWorker.Result> {

    private final ProxyClient client;
    private final int totalTx;
    private final int batchSize;
    private final int workerId; // <--- nouveau

    public OLTPWorker(ProxyClient client, int totalTx, int batchSize, int workerId) {
        this.client = client;
        this.totalTx = totalTx;
        this.batchSize = batchSize;
        this.workerId = workerId;
    }

    @Override
    public Result call() throws Exception {

        List<String> batch = new ArrayList<>(batchSize);
        int doneStatements = 0;
        long startNanos = System.nanoTime();

        int wId = 1;
        int dId = 1;
        int cId = 1;

        for (int i = 0; i < totalTx; i++) {

            // o_id unique par worker + index
            int oId = workerId * 1_000_000 + i;
            int olCnt = 5;

            String orderSql = String.format(
                    "INSERT INTO orders " +
                            "(o_w_id, o_d_id, o_id, o_c_id, o_entry_d, o_carrier_id, o_ol_cnt) " +
                            "VALUES (%d, %d, %d, %d, NOW(), NULL, %d)",
                    wId, dId, oId, cId, olCnt
            );
            batch.add(orderSql);

            for (int line = 1; line <= olCnt; line++) {
                int itemId = 1 + (int) (Math.random() * 100000);
                int qty = 1 + (int) (Math.random() * 10);
                double amount = qty * (1 + Math.random() * 100);

                String olSql = String.format(
                        "INSERT INTO order_line " +
                                "(ol_w_id, ol_d_id, ol_o_id, ol_number, ol_i_id, ol_supply_w_id, ol_quantity, ol_amount) " +
                                "VALUES (%d, %d, %d, %d, %d, %d, %d, %.2f)",
                        wId, dId, oId, line, itemId, wId, qty, amount
                );
                batch.add(olSql);
            }

            if (batch.size() >= batchSize) {
                client.executeBatch(batch);
                doneStatements += batch.size();
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            client.executeBatch(batch);
            doneStatements += batch.size();
        }

        long totalNanos = System.nanoTime() - startNanos;
        return new Result(doneStatements, totalNanos);
    }


    /**
     * Résultat OLTP pour calcul TPS.
     * Ici, on compte les statements SQL exécutés (orders + order_line).
     */
    public static class Result {
        public final int statements;
        public final long totalNanos;

        public Result(int statements, long totalNanos) {
            this.statements = statements;
            this.totalNanos = totalNanos;
        }
    }
}
