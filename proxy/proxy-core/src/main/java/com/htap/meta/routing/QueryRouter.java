package com.htap.meta.routing;

import com.htap.meta.ExecutorFactory;
import com.htap.meta.analyze.QueryAnalyzer;
import com.htap.meta.types.JDBCResultMerger;
import com.htap.meta.ThreadUtils;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.schema.Table;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

/**
 * Routeur de requêtes du proxy HTAP.
 *
 * Cette classe :
 * - délègue l'analyse de la requête à QueryAnalyzer,
 * - choisit l'exécuteur approprié via ExecutorFactory (OLTP ou OLAP),
 * - gère éventuellement un mode hybride en parallélisant les accès,
 * - traite quelques cas particuliers (INSERT ... SELECT) sans parsing,
 * - ajoute automatiquement FINAL aux requêtes OLAP pour ClickHouse ReplacingMergeTree.
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
     *   - OLAP_ONLY : exécution sur la base OLAP (ClickHouse), avec ajout automatique de FINAL.
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
                // Ajouter FINAL pour ClickHouse (ReplacingMergeTree) de façon ciblée
                String sqlForClickHouse = addFinalWithJSqlParser(sql);
                return executorFactory.getOLAPExecutor().execute(sqlForClickHouse);

            case HYBRID:
                Future<Object> fHot = pool.submit((Callable<Object>) () ->
                        executorFactory.getOLTPExecutor().execute(
                                sql + " WHERE ts > NOW() - INTERVAL '1h'")
                );
                Future<Object> fCold = pool.submit((Callable<Object>) () ->
                        executorFactory.getOLAPExecutor().execute(
                                sql + " WHERE ts <= NOW() - INTERVAL '1h'")
                );
                return JDBCResultMerger.merge(fHot.get(), fCold.get());

            default:
                throw new IllegalStateException("Unexpected decision: " + decision);
        }
    }

    /**
     * Ajoute le mot-clé FINAL aux SELECT envoyés à ClickHouse,
     * pour les tables ReplacingMergeTree (CDC Debezium).
     *
     * Implémentation :
     * - Utilise JSqlParser pour identifier la Table du FROM principal (PlainSelect),
     *   puis injecte " FINAL" juste après ce nom de table dans la chaîne originale.
     * - Si FINAL ou SETTINGS final=1 sont déjà présents, ne fait rien.
     * - En cas d'erreur de parsing, renvoie le SQL original.
     */
    private String addFinalWithJSqlParser(String sql) {
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase();

        // On ne traite que les SELECT
        if (!upper.startsWith("SELECT")) {
            return sql;
        }

        // Si FINAL / SETTINGS final=1 déjà présents, on ne modifie pas
        if (upper.contains(" FINAL ")
                || upper.endsWith(" FINAL")
                || upper.contains(" SETTINGS FINAL = 1")
                || upper.contains(" SETTINGS FINAL=1")) {
            return sql;
        }

        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (!(stmt instanceof Select)) {
                return sql;
            }

            Select select = (Select) stmt;
            if (!(select.getSelectBody() instanceof PlainSelect)) {
                // Pour le moment on ne traite que les SELECT simples
                return sql;
            }

            PlainSelect ps = (PlainSelect) select.getSelectBody();
            FromItem from = ps.getFromItem();

            // On ne traite que les FROM "table" simples (pas les sous-select)
            if (from instanceof Table) {
                Table table = (Table) from;

                // Nom tel que JSqlParser le voit (inclut éventuellement l'alias)
                String tableName = table.toString(); // ex: "orders" ou "orders o"
                String sqlUpper = sql.toUpperCase();
                String marker = " FROM " + tableName.toUpperCase();

                int idx = sqlUpper.indexOf(marker);
                if (idx >= 0) {
                    int start = idx + " FROM ".length();
                    int end = start + tableName.length();

                    String before = sql.substring(0, end);
                    String after = sql.substring(end);

                    // Injection de FINAL juste après le nom de table
                    return before + " FINAL" + after;
                }
            }

            return sql;
        } catch (Exception e) {
            // En cas de souci de parsing, on renvoie le SQL original
            return sql;
        }
    }
}
