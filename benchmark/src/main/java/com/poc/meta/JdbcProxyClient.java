package com.poc.meta;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.List;

public class JdbcProxyClient implements ProxyClientInterface, AutoCloseable {

    private final Connection conn;

    public JdbcProxyClient(String jdbcUrl) throws Exception {
        Class.forName("com.htap.meta.HTAPDriver");
        this.conn = DriverManager.getConnection(jdbcUrl);
        this.conn.setAutoCommit(true);
    }

    /** SQL unique sans résultat (INSERT/UPDATE/DELETE, etc.). */
    @Override
    public void executeSingle(String sql) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.setQueryTimeout(30); // 30s sécurité
            st.execute(sql);
        }
    }

    /** Batch OLTP : N statements dans une seule transaction. */
    @Override
    public void executeBatch(List<String> statements) throws Exception {
        try (Statement st = conn.createStatement()) {
            conn.setAutoCommit(false);
            st.setQueryTimeout(60);

            for (String s : statements) {
                st.addBatch(s);
            }

            st.executeBatch();
            conn.commit();

        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException ignore) {}
            throw e;

        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignore) {}
        }
    }

    /** SELECT scalaire int (ex: SELECT count(*)). */
    @Override
    public int executeScalarInt(String sql) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            st.setQueryTimeout(30);

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    /** SELECT scalaire timestamp en millis depuis la colonne 1. */
    @Override
    public Long executeScalarTimestampMillis(String sql) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            st.setQueryTimeout(30);

            if (!rs.next()) return null;

            Object v = rs.getObject(1);
            if (v == null) return null;

            if (v instanceof Number) {
                long n = ((Number) v).longValue();
                return (n < 1_000_000_000_000L) ? n * 1000 : n;
            }

            if (v instanceof Timestamp ts) {
                return ts.getTime();
            }

            // fallback ISO-8601 texte
            OffsetDateTime odt = OffsetDateTime.parse(v.toString());
            return odt.toInstant().toEpochMilli();
        }
    }

    /** Fermeture propre de la connexion */
    @Override
    public void close() throws Exception {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }
}
