package com.htap.meta.routing;

import com.htap.meta.ExecutorFactory;
import com.htap.meta.types.JDBCResultMerger;
import com.htap.meta.analyze.QueryAnalyzer;
import com.htap.meta.ThreadUtils;

import java.util.concurrent.*;
import java.sql.Connection;
import java.sql.Statement;

public class QueryRouter {

    private final QueryAnalyzer analyzer;
    private final ExecutorFactory executorFactory;
    private final ExecutorService pool = ThreadUtils.createFixedThreadPool(8);

    public QueryRouter(QueryAnalyzer analyzer, ExecutorFactory executorFactory) {
        this.analyzer = analyzer;
        this.executorFactory = executorFactory;
    }

    public Object route(String sql) throws Exception {
        String trimmed = sql.trim().toUpperCase();

        // Si c'est un INSERT SELECT complexe, bypass JSQLParser
        if (trimmed.startsWith("INSERT") && trimmed.contains("SELECT")) {
            // Envoie direct sur PostgreSQL (ou ClickHouse si besoin)
            return executorFactory.getOLTPExecutor().executeRaw(sql);
        }

        // Sinon, utilisation normale avec analyse
        QueryRouteDecision decision = analyzer.analyze(sql);

        switch (decision) {
            case OLTP_ONLY:
                return executorFactory.getOLTPExecutor().execute(sql);
            case OLAP_ONLY:
                return executorFactory.getOLAPExecutor().execute(sql);
            case HYBRID:
                Future<Object> fHot = pool.submit(() -> executorFactory.getOLTPExecutor().execute(sql + " WHERE ts > NOW() - INTERVAL '1h'"));
                Future<Object> fCold = pool.submit(() -> executorFactory.getOLAPExecutor().execute(sql + " WHERE ts <= NOW() - INTERVAL '1h'"));
                return JDBCResultMerger.merge(fHot.get(), fCold.get());
            default:
                throw new IllegalStateException("Unexpected decision: " + decision);
        }
    }
}
