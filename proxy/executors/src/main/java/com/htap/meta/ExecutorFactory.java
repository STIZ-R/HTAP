package com.htap.meta;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ExecutorFactory {

    private OLTPExecutor oltpExecutor;
    private OLAPExecutor olapExecutor;

    public ExecutorFactory() throws SQLException {
        // Lire directement depuis les variables d'environnement
        String postgresUrl = System.getenv("POSTGRES_URL");
        String clickhouseUrl = System.getenv("CLICKHOUSE_URL");

        if (postgresUrl == null || clickhouseUrl == null) {
            throw new IllegalStateException("Les URLs des bases ne sont pas définies dans les variables d'environnement.");
        }

        Connection pgConn = DriverManager.getConnection(postgresUrl);
        Connection chConn = DriverManager.getConnection(clickhouseUrl);

        this.oltpExecutor = new OLTPExecutor(pgConn);
        this.olapExecutor = new OLAPExecutor(chConn);
    }

    public OLTPExecutor getOLTPExecutor() {
        return oltpExecutor;
    }

    public OLAPExecutor getOLAPExecutor() {
        return olapExecutor;
    }
}
