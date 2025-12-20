package com.htap.meta;

import com.htap.meta.types.JDBCResultMapper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Exécuteur OLTP pour les requêtes transactionnelles.
 *
 * Cette classe encapsule une connexion JDBC vers la base OLTP (PostgreSQL)
 * et fournit des méthodes pour exécuter des requêtes SQL, soit en renvoyant
 * les résultats structurés, soit en exécutant des commandes sans résultat.
 */
public class OLTPExecutor {

    /**
     * Connexion JDBC vers la base OLTP (PostgreSQL).
     * La durée de vie est gérée par ExecutorFactory, pas fermée ici.
     */
    private final Connection connection;

    /**
     * Construit un exécuteur OLTP à partir d'une connexion JDBC existante.
     *
     * @param connection connexion JDBC déjà ouverte vers la base OLTP
     */
    public OLTPExecutor(Connection connection) {
        this.connection = connection;
    }

    /**
     * Exécute une requête SQL.
     *
     * - SELECT  -> executeQuery + mapping ResultSet -> List<Map<String,Object>>
     * - autres  -> executeUpdate, retourne le nombre de lignes affectées
     */
    public Object execute(String sql) throws Exception {
        String trimmed = sql.trim().toUpperCase();

        try (Statement stmt = connection.createStatement()) {
            if (trimmed.startsWith("SELECT")) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    return JDBCResultMapper.map(rs);
                }
            } else {
                int rows = stmt.executeUpdate(sql);
                return rows; // ou null si tu préfères ignorer ce retour
            }
        }
    }

    /**
     * Exécution brute DML/DDL (INSERT/UPDATE/DELETE/CREATE...), sans mapping de résultat.
     */
    public int executeRaw(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }
}
