package com.htap.meta.analyze;

import com.htap.meta.routing.QueryRouteDecision;

/**
 * Analyseur de requêtes SQL pour le proxy HTAP.
 *
 * Règles simples (POC) :
 * - INSERT / UPDATE / DELETE  -> OLTP
 * - SELECT simple             -> OLAP
 * - SELECT avec hint HYBRID   -> HYBRID
 */
public class QueryAnalyzer {

    public QueryRouteDecision analyze(String sql) {
        String upper = sql.trim().toUpperCase();

        if (upper.startsWith("INSERT")
                || upper.startsWith("UPDATE")
                || upper.startsWith("DELETE")) {
            return QueryRouteDecision.OLTP_ONLY;
        }

        if (upper.contains("HTAP_HYBRID")) {
            return QueryRouteDecision.HYBRID;
        }

        if (upper.startsWith("SELECT")) {
            return QueryRouteDecision.OLAP_ONLY;
        }

        return QueryRouteDecision.OLTP_ONLY;
    }

    /**
     * Ajoute un prédicat "hot" sans casser le SQL.
     */
    public String addHotPredicate(String sql) {
        return addPredicate(sql, "ts > NOW() - INTERVAL '1 hour'");
    }

    /**
     * Ajoute un prédicat "cold" sans casser le SQL.
     */
    public String addColdPredicate(String sql) {
        return addPredicate(sql, "ts <= NOW() - INTERVAL '1 hour'");
    }

    private String addPredicate(String sql, String predicate) {
        String upper = sql.toUpperCase();

        if (upper.contains(" WHERE ")) {
            return sql + " AND " + predicate;
        }

        if (upper.contains(" GROUP BY ")
                || upper.contains(" ORDER BY ")
                || upper.contains(" LIMIT ")) {
            int idx = upper.indexOf(" GROUP BY ");
            if (idx < 0) idx = upper.indexOf(" ORDER BY ");
            if (idx < 0) idx = upper.indexOf(" LIMIT ");

            return sql.substring(0, idx)
                    + " WHERE " + predicate + " "
                    + sql.substring(idx);
        }

        return sql + " WHERE " + predicate;
    }
}
