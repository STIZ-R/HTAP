package com.htap.meta.routing;

import com.htap.meta.ExecutorFactory;
import com.htap.meta.types.JDBCResultMerger;
import com.htap.meta.analyze.QueryAnalyzer;
import com.htap.meta.ThreadUtils;

import java.util.concurrent.*;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Routeur de requêtes du proxy HTAP.
 *
 * Cette classe :
 * - délègue l'analyse de la requête à QueryAnalyzer,
 * - choisit l'exécuteur approprié via ExecutorFactory (OLTP ou OLAP),
 * - gère éventuellement un mode hybride en parallélisant les accès,
 * - traite quelques cas particuliers (INSERT ... SELECT) sans parsing.
 */
public class QueryRouter {

    private final QueryAnalyzer analyzer;
    private final ExecutorFactory executorFactory;
    private final ExecutorService pool = ThreadUtils.createFixedThreadPool(8);

    /**
     * Construit un routeur à partir d'un analyseur et d'une fabrique d'exécuteurs.
     *
     * @param analyzer        composant chargé d'analyser la requête SQL
     * @param executorFactory fabrique fournissant les exécutants OLTP et OLAP
     */
    public QueryRouter(QueryAnalyzer analyzer, ExecutorFactory executorFactory) {
        this.analyzer = analyzer;
        this.executorFactory = executorFactory;
    }

    /**
     * Route et exécute une requête SQL selon la décision de QueryAnalyzer.
     *
     * Stratégie :
     * - Si la requête est un INSERT ... SELECT complexe, on la passe directement
     *   à l'exécuteur OLTP sans parsing (bypass pour simplifier).
     * - Sinon, on analyse la requête pour obtenir une QueryRouteDecision.
     *   - OLTP_ONLY : exécution sur la base OLTP (PostgreSQL).
     *   - OLAP_ONLY : exécution sur la base OLAP (ClickHouse).
     *   - HYBRID    : exécution en parallèle sur OLTP et OLAP, puis fusion
     *                 des résultats via JDBCResultMerger.
     *
     * @param sql requête SQL brute
     * @return le résultat de l'exécution (liste de lignes ou null selon le cas)
     * @throws Exception si l'analyse ou l'exécution de la requête échoue
     */
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
