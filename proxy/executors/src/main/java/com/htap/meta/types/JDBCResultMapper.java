package com.htap.meta.types;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JDBCResultMapper {

    private JDBCResultMapper() {
    }

    /**
     * Transforme un ResultSet en List<Map<String,Object>>.
     */
    public static List<Map<String, Object>> map(ResultSet rs) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= colCount; i++) {
                String colName = meta.getColumnLabel(i);
                Object value = rs.getObject(i);
                row.put(colName, value);
            }
            rows.add(row);
        }
        return rows;
    }
}
