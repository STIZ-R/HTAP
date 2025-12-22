package com.htap.meta;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Fabrique centralisée des exécutants OLTP/OLAP.
 *
 * - Initialise deux pools de connexions JDBC (PostgreSQL et ClickHouse) via HikariCP.
 * - Expose des OLTPExecutor / OLAPExecutor prêts à l'emploi pour le proxy HTAP.
 */
public class ExecutorFactory {

    /** Pool de connexions vers la base transactionnelle (PostgreSQL). */
    private final DataSource oltpDataSource;
    /** Pool de connexions vers la base analytique (ClickHouse). */
    private final DataSource olapDataSource;

    /** Exécuteur dédié aux requêtes OLTP (INSERT/UPDATE/SELECT "chauds"). */
    private final OLTPExecutor oltpExecutor;
    /** Exécuteur dédié aux requêtes OLAP (analytique, agrégations). */
    private final OLAPExecutor olapExecutor;

    /**
     * Construit la fabrique en lisant les URLs dans les variables d'environnement
     * et en configurant les pools HikariCP pour OLTP et OLAP.
     *
     * POSTGRES_URL  → JDBC URL PostgreSQL
     * CLICKHOUSE_URL → JDBC URL ClickHouse
     */
    public ExecutorFactory() {
        String postgresUrl = System.getenv("POSTGRES_URL");
        String clickhouseUrl = System.getenv("CLICKHOUSE_URL");

        if (postgresUrl == null || clickhouseUrl == null) {
            throw new IllegalStateException("POSTGRES_URL ou CLICKHOUSE_URL manquant");
        }

        HikariConfig pgCfg = new HikariConfig();
        pgCfg.setJdbcUrl(postgresUrl);
        pgCfg.setMaximumPoolSize(16);
        pgCfg.setMinimumIdle(4);
        pgCfg.setAutoCommit(true);
        pgCfg.setPoolName("pg-pool");

        this.oltpDataSource = new HikariDataSource(pgCfg);

        HikariConfig chCfg = new HikariConfig();
        chCfg.setJdbcUrl(clickhouseUrl);
        chCfg.setMaximumPoolSize(8);
        chCfg.setMinimumIdle(2);
        chCfg.setPoolName("ch-pool");

        this.olapDataSource = new HikariDataSource(chCfg);

        this.oltpExecutor = new OLTPExecutor(oltpDataSource);
        this.olapExecutor = new OLAPExecutor(olapDataSource);
    }

    /** Retourne l'exécuteur dédié aux requêtes OLTP (PostgreSQL). */
    public OLTPExecutor getOLTPExecutor() {
        return oltpExecutor;
    }

    /** Retourne l'exécuteur dédié aux requêtes OLAP (ClickHouse). */
    public OLAPExecutor getOLAPExecutor() {
        return olapExecutor;
    }
}
