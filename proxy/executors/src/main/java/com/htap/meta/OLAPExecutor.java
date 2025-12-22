package com.htap.meta;

import com.htap.meta.types.JDBCResultMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Exécuteur OLAP pour la base analytique.
 *
 * - Utilise un DataSource (pool Hikari) pour obtenir une connexion ClickHouse.
 * - Exécute des requêtes SELECT et mappe le ResultSet dans une structure Java.
 */
public class OLAPExecutor {

    /** Source de connexions vers la base OLAP (ClickHouse). */
    private final DataSource dataSource;

    /**
     * Construit l'exécuteur OLAP avec un DataSource déjà configuré.
     *
     * @param dataSource pool de connexions vers la base analytique
     */
    public OLAPExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Exécute une requête SQL analytique (typiquement un SELECT) et
     * retourne le résultat sous forme d'objet Java (listes de maps).
     *
     * - Ouvre une connexion depuis le pool.
     * - Crée un Statement.
     * - Exécute la requête et récupère le ResultSet.
     * - Confie le mapping à JDBCResultMapper.
     */
    public Object execute(String sql) throws Exception {
        try (Connection c = dataSource.getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            return JDBCResultMapper.map(rs);
        }
    }
}
