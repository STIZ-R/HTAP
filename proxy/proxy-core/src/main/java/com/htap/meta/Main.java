package com.htap.meta;

import com.htap.meta.analyze.QueryAnalyzer;
import com.htap.meta.config.ProxyConfig;
import com.htap.meta.routing.QueryRouter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {
        ProxyConfig config = new ProxyConfig();
        String pgUrl = config.getPostgresUrl();
        String chUrl = config.getClickhouseUrl();

        Connection pgConn = DriverManager.getConnection(pgUrl);
        Connection chConn = DriverManager.getConnection(chUrl);

        ExecutorFactory executorFactory = new ExecutorFactory();
        QueryRouter router = new QueryRouter(new QueryAnalyzer(), executorFactory);


        Scanner scanner = new Scanner(System.in);
        System.out.println("Proxy HTAP démarré. Tapez SQL ou 'exit' pour quitter.");
        while (true) {
            System.out.print("> ");
            String sql = scanner.nextLine();
            if ("exit".equalsIgnoreCase(sql)) break;

            try {
                Object result = router.route(sql);
                if (result instanceof List) {
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) result;
                    for (Map<String, Object> row : rows) {
                        System.out.println(row);
                    }
                } else {
                    System.out.println(result);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        pgConn.close();
        chConn.close();
        scanner.close();
        System.out.println("Proxy arrêté.");
    }

}
