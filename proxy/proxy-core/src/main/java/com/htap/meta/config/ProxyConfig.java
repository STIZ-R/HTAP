package com.htap.meta.config;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

public class ProxyConfig {
    private String postgresUrl;
    private String clickhouseUrl;

    public ProxyConfig() {
        loadProperties();
    }

    private void loadProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("proxy.properties")) {
            if (is != null) {
                props.load(is);
                postgresUrl = props.getProperty("postgres.url");
                clickhouseUrl = props.getProperty("clickhouse.url");
            } else {
                throw new RuntimeException("proxy.properties not found!");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load proxy.properties", e);
        }
    }

    public String getPostgresUrl() { return postgresUrl; }
    public String getClickhouseUrl() { return clickhouseUrl; }
}
