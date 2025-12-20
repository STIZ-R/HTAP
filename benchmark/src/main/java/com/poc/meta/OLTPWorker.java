package com.poc.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/*
 * Worker OLTP
 *
 * - Accumule les INSERTs en mémoire
 * - Envoie par batch via ProxyClient
 * - Optimisé pour atteindre >10k TPS avec un batch HTTP = N requêtes
 */
public class OLTPWorker implements Callable<OLTPWorker.Result> {

    private final ProxyClient client;
    private final int totalTx;      // nombre total de transactions à exécuter
    private final int batchSize;    // taille de chaque batch

    public OLTPWorker(ProxyClient client, int totalTx, int batchSize) {
        this.client = client;
        this.totalTx = totalTx;
        this.batchSize = batchSize;
    }

    @Override
    public Result call() throws Exception {

        List<String> batch = new ArrayList<>(batchSize);
        int done = 0;
        long startNanos = System.nanoTime();

        for (int i = 0; i < totalTx; i++) {

            // Exemple de requête OLTP
            String sql = String.format(
                    "INSERT INTO orders (user_id, amount) VALUES (%d, %.2f)",
                    1, Math.random() * 100
            );


            batch.add(sql);

            if (batch.size() == batchSize) {
                client.executeBatch(batch);
                done += batch.size();
                batch.clear();
            }
        }

        // Envoyer le dernier batch si incomplet
        if (!batch.isEmpty()) {
            client.executeBatch(batch);
            done += batch.size();
        }

        long totalNanos = System.nanoTime() - startNanos;
        return new Result(done, totalNanos);
    }

    /*
     * Résultat OLTP pour calcul TPS
     */
    public static class Result {
        public final int transactions;
        public final long totalNanos;

        public Result(int transactions, long totalNanos) {
            this.transactions = transactions;
            this.totalNanos = totalNanos;
        }
    }
}
