package com.htap.meta.jdbc;

import com.htap.meta.routing.QueryRouter;
import com.htap.meta.types.JDBCResultMerger;

import java.sql.*;

/**
 * Statement JDBC proxy pour HTAP.
 * Route toutes les requêtes via QueryRouter.
 */
public class Statement implements Statement {

    private final QueryRouter router;

    public Statement(QueryRouter router) {
        this.router = router;
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        try {
            Object result = router.route(sql);
            if (result instanceof ResultSet) return (ResultSet) result;
            throw new SQLException("Expected ResultSet, got: " + result);
        } catch (Exception e) {
            throw new SQLException("Failed to route query", e);
        }
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        try {
            Object result = router.route(sql);
            if (result instanceof Integer) return (Integer) result;
            return 0;
        } catch (Exception e) {
            throw new SQLException("Failed to route update", e);
        }
    }

    @Override
    public void close() throws SQLException {}

    // --- Stubs pour les autres méthodes Statement
    @Override public boolean execute(String sql) { throw new UnsupportedOperationException(); }
    @Override public ResultSet getResultSet() { throw new UnsupportedOperationException(); }
    @Override public int getUpdateCount() { throw new UnsupportedOperationException(); }
    @Override public boolean getMoreResults() { throw new UnsupportedOperationException(); }
    @Override public void setFetchDirection(int direction) {}
    @Override public int getFetchDirection() { return ResultSet.FETCH_FORWARD; }
    @Override public void setFetchSize(int rows) {}
    @Override public int getFetchSize() { return 0; }
    @Override public int getResultSetConcurrency() { return ResultSet.CONCUR_READ_ONLY; }
    @Override public int getResultSetType() { return ResultSet.TYPE_FORWARD_ONLY; }
    @Override public Connection getConnection() { return null; }
    @Override public boolean getMoreResults(int current) { throw new UnsupportedOperationException(); }
    @Override public void addBatch(String sql) { throw new UnsupportedOperationException(); }
    @Override public void clearBatch() { throw new UnsupportedOperationException(); }
    @Override public int[] executeBatch() { throw new UnsupportedOperationException(); }
    @Override public void cancel() { throw new UnsupportedOperationException(); }
    @Override public SQLWarning getWarnings() { return null; }
    @Override public void clearWarnings() {}
    @Override public void setMaxFieldSize(int max) {}
    @Override public int getMaxFieldSize() { return 0; }
    @Override public void setMaxRows(int max) {}
    @Override public int getMaxRows() { return 0; }
    @Override public void setQueryTimeout(int seconds) {}
    @Override public int getQueryTimeout() { return 0; }
    @Override public void setEscapeProcessing(boolean enable) {}
    @Override public int getResultSetHoldability() { return ResultSet.CLOSE_CURSORS_AT_COMMIT; }
    @Override public boolean isClosed() { return false; }
    @Override public void setPoolable(boolean poolable) {}
    @Override public boolean isPoolable() { return false; }
    @Override public void closeOnCompletion() {}
    @Override public boolean isCloseOnCompletion() { return false; }
    @Override public <T> T unwrap(Class<T> iface) { throw new UnsupportedOperationException(); }
    @Override public boolean isWrapperFor(Class<?> iface) { return false; }
}
