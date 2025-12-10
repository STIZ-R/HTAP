package com.htap.meta;

import java.sql.*;
import java.util.*;

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
     * Exécute une requête SQL qui renvoie un jeu de résultats (ResultSet)
     * et transforme ce résultat en une liste de lignes représentées par des maps.
     *
     * Chaque ligne est un Map où :
     * - la clé est le nom de la colonne (column label),
     * - la valeur est la valeur correspondante dans le ResultSet.
     *
     * @param sql requête SQL à exécuter (typiquement SELECT)
     * @return une liste de lignes, chacune représentée par un Map colonne → valeur
     * @throws SQLException si l'exécution de la requête échoue
     */
    public List<Map<String, Object>> execute(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<Map<String, Object>> results = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                results.add(row);
            }
            return results;
        }
    }

    /**
     * Exécute une requête SQL sans récupérer ni exploiter de résultat.
     *
     * Cette méthode convient aux commandes dont on ne lit pas les lignes
     * de retour (DDL, INSERT/UPDATE/DELETE, etc.).
     *
     * @param sql requête SQL à exécuter
     * @return toujours null, aucune donnée n'est renvoyée
     * @throws Exception si l'exécution échoue
     */
    public Object executeRaw(String sql) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            return null;
        }
    }

}
