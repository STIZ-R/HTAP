package com.htap.meta;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLUtils {

    public static String resultSetToString(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder();
        int columnCount = rs.getMetaData().getColumnCount();
        while(rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                sb.append(rs.getString(i)).append("\t");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
