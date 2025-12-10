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
    public QueryRouteDecision analyze(String sql) throws Exception {
        Statement stmt = CCJSqlParserUtil.parse(sql);

        if (stmt instanceof Insert || stmt instanceof Update || stmt instanceof Delete) {
            return QueryRouteDecision.OLTP_ONLY;
        }
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            String lower = select.toString().toLowerCase();
            if (lower.contains("group by") || lower.matches(".*(sum|avg|count|min|max)\\(.*\\).*")) {
                return QueryRouteDecision.OLAP_ONLY;
            }
        }
        return QueryRouteDecision.OLTP_ONLY;
    }
}
