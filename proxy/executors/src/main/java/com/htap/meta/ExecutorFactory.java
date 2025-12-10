package com.htap.meta;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Fabrique d'exécuteurs pour le proxy HTAP.
 *
 * Cette classe est responsable de :
 * - Lire les URLs de connexion PostgreSQL et ClickHouse depuis les variables d'environnement.
 * - Ouvrir les connexions JDBC vers les deux bases.
 * - Instancier et exposer les exécutants OLTP et OLAP correspondants.
 *
 * Elle centralise ainsi toute la logique d'initialisation des connexions
 * et fournit aux autres composants (router, proxy) des exécutants prêts à l'emploi.
 */
public class ExecutorFactory {

    private OLTPExecutor oltpExecutor;
    private OLAPExecutor olapExecutor;

    /**
     * Constructeur lisant les URLs provenant des variables d'environnement
     * et ouvre 2 connexions via JDBC
     *
     * @throws SQLException retoure une erreur si les URLs ne sont pas définis
     */
    public ExecutorFactory() throws SQLException {
        String postgresUrl = System.getenv("POSTGRES_URL");
        String clickhouseUrl = System.getenv("CLICKHOUSE_URL");

        if (postgresUrl == null || clickhouseUrl == null) {
            throw new IllegalStateException("Les URLs des bases ne sont pas définies dans les variables d'environnement.");
        }

        Connection pgConn = DriverManager.getConnection(postgresUrl);
        Connection chConn = DriverManager.getConnection(clickhouseUrl);

        this.oltpExecutor = new OLTPExecutor(pgConn);
        this.olapExecutor = new OLAPExecutor(chConn);
    }

    /**
     * Retourne l'exécuteur dédié aux requêtes OLTP (PostgreSQL).
     *
     * @return l'instance d'OLTPExecutor initialisée avec la connexion PostgreSQL
     */
    public OLTPExecutor getOLTPExecutor() {
        return oltpExecutor;
    }

    /**
     * Retourne l'exécuteur dédié aux requêtes OLAP (ClickHouse).
     *
     * @return l'instance d'OLAPExecutor initialisée avec la connexion ClickHouse
     */
    public OLAPExecutor getOLAPExecutor() {
        return olapExecutor;
    }
}
