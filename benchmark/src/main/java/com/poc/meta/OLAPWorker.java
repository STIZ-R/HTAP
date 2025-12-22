package com.poc.meta;

/**
 * Worker OLAP :
 *
 * - Envoie périodiquement une requête analytique.
 * - Sert à tester la coexistence OLTP / OLAP via le proxy.
 */
public class OLAPWorker implements Runnable {

    private final ProxyClient client;

    public OLAPWorker(ProxyClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            while (true) {
                client.executeSingle(
                        "SELECT ol_w_id AS o_w_id, ol_d_id AS o_d_id, ol_o_id AS o_c_id, " +
                                "       SUM(ol_amount) AS revenue, COUNT(*) AS lines " +
                                "FROM order_line " +
                                "GROUP BY ol_w_id, ol_d_id, ol_o_id " +
                                "ORDER BY revenue DESC " +
                                "LIMIT 10"
                );

                Thread.sleep(500);
            }
        } catch (Exception ignored) {
        }
    }
}
