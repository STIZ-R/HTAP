package com.htap.meta.jdbc;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Driver JDBC custom pour le proxy HTAP.
 * Permet aux benchmarks de se connecter comme s'ils parlaient directement à une base.
 */
public class Driver implements Driver {

    static {
        try {
            DriverManager.registerDriver(new Driver());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register HtapProxyDriver", e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) return null;
        return new Connection(info);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url != null && url.startsWith("jdbc:htapproxy:");
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() { return 1; }

    @Override
    public int getMinorVersion() { return 0; }

    @Override
    public boolean jdbcCompliant() { return false; }

    @Override
    public Logger getParentLogger() {
        return Logger.getGlobal();
    }
}
