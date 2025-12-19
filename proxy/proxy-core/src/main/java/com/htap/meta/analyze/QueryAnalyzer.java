package com.htap.meta.analyze;

import com.htap.meta.routing.QueryRouteDecision;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.delete.Delete;


/**
 * Analyseur de requêtes SQL pour le proxy HTAP.
 *
 * Utilise JSqlParser pour parser la requête et déterminer le type de charge :
 * - requêtes de modification de données (INSERT, UPDATE, DELETE) → OLTP_ONLY,
 * - requêtes SELECT simples → OLTP_ONLY par défaut,
 * - requêtes SELECT avec agrégations ou GROUP BY → OLAP_ONLY.
 */
public class QueryAnalyzer {

    /**
     * Analyse la requête SQL et renvoie une décision de routage.
     *
     * @param sql requête SQL brute
     * @return une décision de type QueryRouteDecision (OLTP_ONLY ou OLAP_ONLY)
     * @throws Exception si le parsing de la requête échoue
     */
//    public QueryRouteDecision analyze(String sql) throws Exception {
//        Statement stmt = CCJSqlParserUtil.parse(sql);
//
//        if (stmt instanceof Insert || stmt instanceof Update || stmt instanceof Delete) {
//            return QueryRouteDecision.OLTP_ONLY;
//        }
//        if (stmt instanceof Select) {
//            Select select = (Select) stmt;
//            String lower = select.toString().toLowerCase();
//            if (lower.contains("group by") || lower.matches(".*(sum|avg|count|min|max)\\(.*\\).*")) {
//                return QueryRouteDecision.OLAP_ONLY;
//            }
//        }
//        return QueryRouteDecision.OLTP_ONLY;
//    }
    public QueryRouteDecision analyze(String sql) throws Exception {
        Statement stmt = CCJSqlParserUtil.parse(sql);
        String lower = sql.toLowerCase();

        // --- 1. Toute modification = OLTP
        if (stmt instanceof Insert || stmt instanceof Update || stmt instanceof Delete) {
            return QueryRouteDecision.OLTP_ONLY;
        }

        if (stmt instanceof Select) {

            // --- 2. SELECT à clé → OLTP
            if (lower.matches(".*where\\s+.*id\\s*=.*")) {
                return QueryRouteDecision.OLTP_ONLY;
            }

            // --- 3. Requêtes analytiques classiques (OLAP)
            if (lower.contains("group by") ||
                    lower.contains("having") ||
                    lower.contains("distinct") ||
                    lower.contains("union") ||
                    lower.contains("join") ||        // TPC-H contient toujours des JOIN
                    lower.contains("order by") ||
                    lower.contains("limit") && !lower.matches(".*limit\\s+1.*") ||
                    lower.matches(".*(sum|avg|count|min|max)\\s*\\(.*\\).*")) {

                return QueryRouteDecision.OLAP_ONLY;
            }

            // --- 4. Sous-requêtes = OLAP
            if (lower.contains("select") && lower.contains("from") && lower.contains("(")) {
                return QueryRouteDecision.OLAP_ONLY;
            }

            // --- 5. Par défaut, OLTP
            return QueryRouteDecision.OLTP_ONLY;
        }

        // --- 6. Fallback
        return QueryRouteDecision.OLTP_ONLY;
    }

}
