package com.htap.meta;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Utilitaires liés au SQL et aux ResultSet.
 *
 * Contient des méthodes pratiques pour transformer ou inspecter
 * les résultats JDBC (ResultSet).
 */
public class SQLUtils {

    /**
     * Convertit un ResultSet en chaîne de caractères lisible.
     *
     * Les valeurs de chaque ligne sont séparées par des tabulations,
     * et chaque ligne est terminée par un saut de ligne.
     *
     * @param rs ResultSet à convertir
     * @return représentation textuelle du ResultSet
     * @throws SQLException si l'accès aux métadonnées ou aux colonnes échoue
     */
    public static String resultSetToString(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder();
        int columnCount = rs.getMetaData().getColumnCount();
        while(rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                sb.append(rs.getString(i)).append("\t");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
