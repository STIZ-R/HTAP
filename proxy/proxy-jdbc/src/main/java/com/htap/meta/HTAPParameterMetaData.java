package com.htap.meta;

import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.sql.Types;

public class HTAPParameterMetaData implements ParameterMetaData {

    private final int count;
    private static final int parameterModeIn = ParameterMetaData.parameterModeIn;
    private static final int parameterNullable = ParameterMetaData.parameterNullable;


    public HTAPParameterMetaData(int count) {
        this.count = count;
    }

    @Override
    public int getParameterCount() {
        return count;
    }

    @Override
    public int getParameterType(int param) {
        return Types.JAVA_OBJECT;
    }

    @Override
    public String getParameterTypeName(int param) {
        return "JAVA_OBJECT";
    }

    @Override
    public int getParameterMode(int param) {
        return parameterModeIn;
    }

    @Override
    public int isNullable(int param) {
        return parameterNullable;
    }

    @Override
    public boolean isSigned(int param) {
        return true;
    }

    @Override
    public int getPrecision(int param) {
        return 0;
    }

    @Override
    public int getScale(int param) {
        return 0;
    }

    @Override
    public String getParameterClassName(int param) {
        return Object.class.getName();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("Not a wrapper");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return false;
    }
}
