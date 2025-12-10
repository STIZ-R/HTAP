package com.htap.meta;

import java.sql.*;
import java.util.*;

/**
 * Exécuteur OLAP pour les requêtes analytiques.
 *
 * Cette classe encapsule une connexion JDBC vers la base analytique (ClickHouse)
 * et fournit des méthodes pour exécuter des requêtes SQL, soit en renvoyant
 * les résultats structurés, soit en exécutant des commandes sans résultat.
 */
public class OLAPExecutor {

    /**
     * Connexion JDBC vers la base OLAP (ClickHouse).
     */
    private final Connection connection;

    /**
     * Construit un exécuteur OLAP à partir d'une connexion JDBC existante.
     *
     * @param connection connexion JDBC déjà ouverte vers la base OLAP
     */
    public OLAPExecutor(Connection connection) {
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
     * Exécute une requête SQL sans s'intéresser au résultat.
     *
     * Cette méthode est adaptée aux commandes qui ne renvoient pas de ResultSet
     * ou dont le résultat n'est pas exploité (DDL, INSERT/UPDATE/DELETE, etc.).
     *
     * @param sql requête SQL à exécuter
     * @return toujours null, aucune donnée de résultat n'est renvoyée
     * @throws Exception si l'exécution échoue
     */
    public Object executeRaw(String sql) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            return null;
        }
    }

}
