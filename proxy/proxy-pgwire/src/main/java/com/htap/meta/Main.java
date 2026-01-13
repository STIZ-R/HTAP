package com.htap.meta;

import com.htap.meta.analyze.QueryAnalyzer;
import com.htap.meta.routing.QueryRouter;
import com.htap.meta.ExecutorFactory;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        ExecutorFactory factory = new ExecutorFactory();
        QueryAnalyzer analyzer = new QueryAnalyzer();
        QueryRouter router = new QueryRouter(analyzer, factory);

        int port = 5432; // port PostgreSQL standard
        PgWireServer server = new PgWireServer(port, router);

        System.out.println("PgWire HTAP proxy listening on port " + port);
        server.start();
    }
}
