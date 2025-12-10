package com.htap.meta.types;

import java.util.*;

/**
 * Utilitaire pour fusionner deux résultats JDBC.
 *
 * Cette classe fournit une méthode statique permettant de fusionner
 * deux jeux de résultats représentés sous forme de listes de maps.
 * Elle est utile lorsque des données "chaudes" et "froides" doivent
 * être combinées dans un seul résultat logique.
 */
public class JDBCResultMerger {

    /**
     * Fusionne deux objets de résultat en une seule liste.
     *
     * Les paramètres hot et cold sont supposés être des List<Map<String, Object>>,
     * et sont simplement concaténés dans une nouvelle liste.
     *
     * @param hot  partie "chaude" du résultat
     * @param cold partie "froide" du résultat
     * @return une nouvelle liste contenant d'abord les lignes de hot, puis celles de cold
     */
    public static List<Map<String, Object>> merge(Object hot, Object cold) {
        List<Map<String, Object>> merged = new ArrayList<>();
        merged.addAll((List<Map<String, Object>>) hot);
        merged.addAll((List<Map<String, Object>>) cold);
        return merged;
    }
}
