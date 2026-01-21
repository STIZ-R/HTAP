package com.htap.meta;

import java.sql.*;
import java.util.Properties;

public class HTAPDriver implements Driver {

    static {
        try {
            DriverManager.registerDriver(new HTAPDriver());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) return null;

        String endpoint = url.substring("jdbc:htap:".length());

        return new HTAPConnection(endpoint);
    }


    @Override
    public boolean acceptsURL(String url) {
        return url != null && url.startsWith("jdbc:htap:");
    }

    @Override public int getMajorVersion() { return 1; }
    @Override public int getMinorVersion() { return 0; }
    @Override public boolean jdbcCompliant() { return false; }
    @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }
    @Override public java.util.logging.Logger getParentLogger() {
        return java.util.logging.Logger.getGlobal();
    }
}
