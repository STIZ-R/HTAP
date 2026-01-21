package com.htap.meta;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class HTAPConnection implements Connection {

    final String endpoint;
    boolean autoCommit = true;
    boolean closed = false;

    HTAPConnection(String endpoint) {
        this.endpoint = endpoint;
    }

    private void checkOpen() throws SQLException {
        if (closed) throw new SQLException("Connection is closed");
    }


    @Override
    public Statement createStatement() {
        return new HTAPStatement(this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) {
        return new HTAPPreparedStatement(this, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return null;
    }

    @Override
    public String nativeSQL(String sql) {
        return sql;
    }


    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        checkOpen();

        if (this.autoCommit == autoCommit) return;

        this.autoCommit = autoCommit;

        if (!autoCommit) {
            // début transaction
            HttpClient.post(endpoint, "/proxy/query", "BEGIN");
        } else {
            // fin implicite transaction
            HttpClient.post(endpoint, "/proxy/query", "COMMIT");
        }
    }


    @Override
    public boolean getAutoCommit() throws SQLException {
        return autoCommit;
    }


    @Override
    public void commit() throws SQLException {
        checkOpen();

        if (!autoCommit) {
            HttpClient.post(endpoint, "/proxy/query", "COMMIT");
            // redémarre une transaction implicite
            HttpClient.post(endpoint, "/proxy/query", "BEGIN");
        }
    }

    @Override
    public void rollback() throws SQLException {
        checkOpen();

        if (!autoCommit) {
            HttpClient.post(endpoint, "/proxy/query", "ROLLBACK");
            HttpClient.post(endpoint, "/proxy/query", "BEGIN");
        }
    }

    @Override
    public void close() {
        if (!closed) {
            try {
                HttpClient.post(endpoint, "/proxy/close", "");
            } catch (Exception ignore) {}
            closed = true;
        }
    }


    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return new HTAPDatabaseMetaData(this);
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {

    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return false;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {

    }

    @Override
    public String getCatalog() throws SQLException {
        return "";
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {

    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return 0;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return createStatement(); // renvoie le Statement normal
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return prepareStatement(sql); // idem
    }


    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return Map.of();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {

    }

    @Override
    public void setHoldability(int holdability) throws SQLException {

    }

    @Override
    public int getHoldability() throws SQLException {
        return 0;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return null;
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return null;
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {

    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {

    }

    @Override
    public Statement createStatement(int t, int c, int h) {
        return createStatement();
    }


    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return new HTAPPreparedStatement(this, sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return null;
    }

    @Override
    public Clob createClob() throws SQLException {
        return null;
    }

    @Override
    public Blob createBlob() throws SQLException {
        return null;
    }

    @Override
    public NClob createNClob() throws SQLException {
        return null;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return null;
    }

    @Override
    public boolean isValid(int timeout) {
        try {
            Object res = HttpClient.post(endpoint, "/proxy/query", "SELECT 1");
            return res != null;
        } catch (Exception e) {
            return false;
        }
    }



    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {

    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {

    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return "";
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return null;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return null;
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return null;
    }

    @Override
    public void setSchema(String schema) throws SQLException {

    }

    @Override
    public String getSchema() throws SQLException {
        return "";
    }

    @Override
    public void abort(Executor executor) throws SQLException {

    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {

    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return 0;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    /* Le reste : no-op ou UnsupportedOperationException */
}
