package com.poc.meta;

public interface ProxyClientInterface {
    void executeSingle(String sql) throws Exception;
    void executeBatch(java.util.List<String> statements) throws Exception;
    int executeScalarInt(String sql) throws Exception;
    Long executeScalarTimestampMillis(String sql) throws Exception;
}
