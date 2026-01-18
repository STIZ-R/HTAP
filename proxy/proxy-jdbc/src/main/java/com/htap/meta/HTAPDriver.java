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
    public boolean acceptsURL(String url) {
        return url.startsWith("jdbc:htap:");
    }

    @Override
    public Connection connect(String url, Properties info) {
        // jdbc:htap:http://localhost:8080
        String endpoint = url.substring("jdbc:htap:".length());
        return new HTAPConnection(endpoint);
    }

    /* Boilerplate inutile pour HTAPBench */
    public int getMajorVersion() { return 1; }
    public int getMinorVersion() { return 0; }
    public boolean jdbcCompliant() { return false; }
    public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) { return new DriverPropertyInfo[0]; }
    public java.util.logging.Logger getParentLogger() { return null; }
}
