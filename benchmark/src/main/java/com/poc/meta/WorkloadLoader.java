package com.poc.meta;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Charge des fichiers .sql depuis le classpath.
 * - Les lignes vides ou commençant par "--" sont ignorées.
 * - Un point-virgule ';' termine une requête.
 */
public class WorkloadLoader {

    public static List<String> loadSqlFile(String resourcePath) throws Exception {
        List<String> queries = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        try (InputStream is = WorkloadLoader.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("--")) {
                        continue;
                    }
                    current.append(line).append(' ');
                    if (line.endsWith(";")) {
                        String sql = current.toString();
                        sql = sql.substring(0, sql.length() - 1).trim();
                        queries.add(sql);
                        current.setLength(0);
                    }
                }
            }
        }
        return queries;
    }
}
