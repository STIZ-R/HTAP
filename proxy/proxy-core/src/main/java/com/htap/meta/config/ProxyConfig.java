package com.htap.meta.config;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

/**
 * Chargement de la configuration du proxy HTAP.
 *
 * Cette classe lit le fichier proxy.properties présent sur le classpath
 * pour récupérer les URLs JDBC de PostgreSQL et de ClickHouse.
 */
public class ProxyConfig {
    private String postgresUrl;
    private String clickhouseUrl;

    /**
     * Construit la configuration en chargeant le fichier proxy.properties.
     */
    public ProxyConfig() {
        loadProperties();
    }

    /**
     * Charge les propriétés depuis proxy.properties.
     *
     * @throws RuntimeException si le fichier est introuvable ou ne peut pas être lu
     */
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

    /**
     * @return l'URL JDBC de PostgreSQL lue dans proxy.properties
     */
    public String getPostgresUrl() { return postgresUrl; }

    /**
     * @return l'URL JDBC de ClickHouse lue dans proxy.properties
     */
    public String getClickhouseUrl() { return clickhouseUrl; }
}
