package com.htap.meta.analyze;

import com.htap.meta.routing.QueryRouteDecision;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.delete.Delete;

public class QueryAnalyzer {

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
