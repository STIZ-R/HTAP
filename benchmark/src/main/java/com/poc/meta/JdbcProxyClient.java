package com.poc.meta;

import java.sql.*;
import java.time.OffsetDateTime;

public class JdbcProxyClient implements ProxyClientInterface{

    private final Connection conn;

    public JdbcProxyClient(String jdbcUrl) throws Exception {
        Class.forName("com.htap.meta.HTAPDriver");
        this.conn = DriverManager.getConnection(jdbcUrl);
    }

    /** SQL unique sans résultat (INSERT/UPDATE/DELETE, etc.). */
    public void executeSingle(String sql) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    /** Batch OLTP : on envoie N statements dans une seule transaction. */
    public void executeBatch(java.util.List<String> statements) throws Exception {
        try (Statement st = conn.createStatement()) {
            conn.setAutoCommit(false);
            for (String s : statements) {
                st.addBatch(s);
            }
            st.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        }
    }

    /** SELECT scalaire int (ex: SELECT count(*)). */
    public int executeScalarInt(String sql) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    /** SELECT scalaire timestamp en millis depuis la colonne 1. */
    public Long executeScalarTimestampMillis(String sql) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (!rs.next()) return null;

            Object v = rs.getObject(1);
            if (v == null) return null;

            if (v instanceof Number) {
                long n = ((Number) v).longValue();
                return (n < 1_000_000_000_000L) ? n * 1000 : n;
            }
            if (v instanceof java.sql.Timestamp ts) {
                return ts.getTime();
            }
            // fallback texte type ISO
            OffsetDateTime odt = OffsetDateTime.parse(v.toString());
            return odt.toInstant().toEpochMilli();
        }
    }
}
