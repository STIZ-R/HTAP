package com.htap.meta;

import com.htap.meta.types.JDBCResultMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * Exécuteur OLTP pour la base transactionnelle.
 *
 * - S'appuie sur un DataSource Hikari pour PostgreSQL.
 * - Gère des requêtes unitaires (SELECT / INSERT / UPDATE / DELETE).
 * - Fournit un mode batch pour exécuter plusieurs statements dans une seule transaction.
 */
public class OLTPExecutor {

    /** Source de connexions vers la base OLTP (PostgreSQL). */
    private final DataSource dataSource;

    /**
     * Construit l'exécuteur OLTP avec un DataSource déjà configuré.
     *
     * @param dataSource pool de connexions vers la base transactionnelle
     */
    public OLTPExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Exécute une requête SQL unique.
     *
     * - Si la requête commence par SELECT → executeQuery + mapping du ResultSet.
     * - Sinon → executeUpdate et retourne le nombre de lignes affectées.
     *
     * Cette méthode est pensée pour les appels "unitaires" OLTP via le proxy.
     */
    public Object execute(String sql) throws Exception {
        String trimmed = sql.trim().toUpperCase();

        try (Connection c = dataSource.getConnection();
             Statement stmt = c.createStatement()) {

            if (trimmed.startsWith("SELECT")) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    return JDBCResultMapper.map(rs);
                }
            } else {
                return stmt.executeUpdate(sql);
            }
        }
    }

    /**
     * Exécute une liste de statements SQL dans une seule transaction via JDBC batch.
     *
     * - Désactive l'auto-commit pour grouper toutes les requêtes.
     * - Ajoute chaque statement au batch.
     * - Exécute le batch puis fait un COMMIT.
     *
     * Adapté pour les gros volumes d'INSERT / UPDATE afin d'augmenter le TPS.
     */
    public void executeBatch(List<String> sqls) throws Exception {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);

            try (Statement st = c.createStatement()) {
                for (String sql : sqls) {
                    st.addBatch(sql);
                }
                st.executeBatch();
            }

            c.commit();
        }
    }

    /**
     * Exécution "brute" d'un statement DML/DDL, sans mapping de résultat.
     *
     * - Idéal pour les INSERT / UPDATE / DELETE / CREATE... où seul
     *   le nombre de lignes affectées compte.
     *
     * @return nombre de lignes affectées
     */
    public int executeRaw(String sql) throws Exception {
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement()) {
            return st.executeUpdate(sql);
        }
    }
}
