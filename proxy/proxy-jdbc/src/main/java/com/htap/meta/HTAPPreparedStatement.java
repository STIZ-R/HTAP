package com.htap.meta;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTAPPreparedStatement extends HTAPStatement implements PreparedStatement {

    private final String template;
    private final Map<Integer,Object> params = new HashMap<>();

    HTAPPreparedStatement(HTAPConnection conn, String sql) {
        super(conn);
        this.template = sql;
    }

    @Override public void setInt(int i, int v) { params.put(i, v); }
    @Override public void setLong(int i, long v) { params.put(i, v); }
    @Override public void setString(int i, String v) { params.put(i, "'" + v + "'"); }


    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {

    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {

    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {

    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {

    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {

    }

    @Override
    public void setDate(int i, Date d) {
        params.put(i, "'" + d.toString() + "'");
    }

    @Override
    public void setTimestamp(int i, Timestamp ts) {
        params.put(i, "'" + ts.toString() + "'");
    }


    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {

    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {

    }

    @Override
    public void clearParameters() {
        params.clear();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {

    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        if (x == null) {
            params.put(parameterIndex, "null");
        } else if (x instanceof String) {
            params.put(parameterIndex, "'" + x + "'");
        } else {
            params.put(parameterIndex, x);
        }
    }


    @Override
    public boolean execute() throws SQLException {
        String boundSql = bind();
        Object res = HttpClient.post(conn.endpoint, "/proxy/query", boundSql);
        List<Map<String,Object>> rows = HTAPStatement.adaptResult(res);

        if (rows != null && !rows.isEmpty()) {
            resultSet = new HTAPResultSet(rows, this);
            updateCount = -1;
            return true;   // result set
        }

        resultSet = null;
        updateCount = (res instanceof Number) ? ((Number) res).intValue() : 0;
        return false;      // update count
    }






    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {

    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {

    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {

    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {

    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {

    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return new HTAPResultSetMetaData(Collections.emptyList());
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {

    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {

    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {

    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {

    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {

    }

    @Override
    public ParameterMetaData getParameterMetaData() {
        long count = template.chars().filter(c -> c == '?').count();
        return new HTAPParameterMetaData((int) count);
    }


    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {

    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {

    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {

    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {

    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {

    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {

    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {

    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {

    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {

    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {

    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {

    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {

    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {

    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        String boundSql = bind();
        Object res = HttpClient.post(conn.endpoint, "/proxy/query", boundSql);
        List<Map<String,Object>> rows = HTAPStatement.adaptResult(res);
        resultSet = new HTAPResultSet(rows, this);
        updateCount = -1;   // IMPORTANT JDBC
        return resultSet;

    }


    @Override
    public int executeUpdate() throws SQLException {
        execute();
        return updateCount;
    }


    @Override
    public void setNull(int parameterIndex, int sqlType) {
        params.put(parameterIndex, "null");
    }


    @Override public void setBoolean(int i, boolean v) { params.put(i, v); }
    @Override public void setByte(int i, byte v) { params.put(i, v); }
    @Override public void setShort(int i, short v) { params.put(i, v); }


    private String bind() throws SQLException {
        StringBuilder sql = new StringBuilder();
        int lastIndex = 0;
        int paramIndex = 1;
        Matcher m = Pattern.compile("\\?").matcher(template);
        while (m.find()) {
            sql.append(template, lastIndex, m.start());
            if (!params.containsKey(paramIndex))
                throw new SQLException("Missing parameter " + paramIndex);
            Object v = params.get(paramIndex++);
            sql.append(v == null ? "null" : v.toString());
            lastIndex = m.end();
        }
        sql.append(template.substring(lastIndex));
        return sql.toString();
    }




    private boolean closed = false;

    @Override
    public void close() {
        closed = true;
        params.clear();
        batch.clear();
        resultSet = null;
    }
    @Override
    public int getMaxFieldSize() throws SQLException {
        return 0;
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {

    }

    @Override
    public int getMaxRows() throws SQLException {
        return 0;
    }

    @Override
    public void setMaxRows(int max) throws SQLException {

    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {

    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return 0;
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {

    }

    @Override
    public void cancel() throws SQLException {

    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {

    }

    @Override
    public void setCursorName(String name) throws SQLException {

    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return resultSet;
    }


    @Override
    public int getUpdateCount() throws SQLException {
        return updateCount;
    }


    @Override
    public boolean getMoreResults() throws SQLException {
        return false;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {

    }

    @Override
    public int getFetchDirection() throws SQLException {
        return 0;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {

    }

    @Override
    public int getFetchSize() throws SQLException {
        return 0;
    }

    @Override
    public int getResultSetConcurrency() {
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public int getResultSetType() {
        return ResultSet.TYPE_FORWARD_ONLY;
    }




    @Override
    public void addBatch() throws SQLException {
        batch.add(bind());
        params.clear();
    }

    @Override
    public void clearBatch() {
        batch.clear();
    }

    @Override
    public int[] executeBatch() {
        return super.executeBatch();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return conn;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return false;
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return null;
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return 0;
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return 0;
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return 0;
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return false;
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return false;
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return false;
    }

    @Override
    public int getResultSetHoldability() {
        return 0;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {

    }

    @Override
    public boolean isPoolable() throws SQLException {
        return false;
    }

    @Override
    public void closeOnCompletion() throws SQLException {

    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return false;
    }
}
