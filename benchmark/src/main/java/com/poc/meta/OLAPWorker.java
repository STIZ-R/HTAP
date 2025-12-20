package com.poc.meta;

/*
 * Worker OLAP :
 *
 * - Envoie des SELECT périodiques
 * - Sert à tester la coexistence OLTP / OLAP
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
                        "SELECT user_id, SUM(amount) FROM orders GROUP BY user_id LIMIT 10"
                );
                Thread.sleep(500); /* pause pour ne pas saturer */
            }
        } catch (Exception ignored) {
        }
    }
}
