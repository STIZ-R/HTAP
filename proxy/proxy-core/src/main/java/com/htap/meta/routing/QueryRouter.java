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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

/**
 * Routeur de requêtes du proxy HTAP.
 *
 * - Analyse chaque requête SQL pour décider si elle doit aller en OLTP, OLAP ou en mode hybride.
 * - Délègue ensuite l'exécution aux exécutants appropriés (PostgreSQL / ClickHouse).
 * - Gère la parallélisation des requêtes hybrides via un pool de threads.
 */
public class QueryRouter {

    /** Composant chargé de classifier et réécrire les requêtes (OLTP / OLAP / HYBRID). */
    private final QueryAnalyzer analyzer;
    /** Fabrique fournissant les exécutants JDBC vers OLTP et OLAP. */
    private final ExecutorFactory executorFactory;
    /** Pool de threads pour exécuter en parallèle les parties chaudes/froides des requêtes hybrides. */
    private final ExecutorService pool = ThreadUtils.createFixedThreadPool(8);

    /**
     * Construit un routeur HTAP avec un analyseur et une fabrique d'exécutants.
     *
     * @param analyzer analyseur de requêtes SQL (décision de routage, réécriture)
     * @param executorFactory fabrique d'exécutants OLTP/OLAP
     */
    public QueryRouter(QueryAnalyzer analyzer, ExecutorFactory executorFactory) {
        this.analyzer = analyzer;
        this.executorFactory = executorFactory;
    }

    /**
     * Route et exécute une requête SQL unique.
     *
     * Étapes principales :
     * - Cas particulier : bypass des INSERT ... SELECT complexes directement vers OLTP.
     * - Analyse de la requête pour décider OLTP_ONLY / OLAP_ONLY / HYBRID.
     * - Exécution :
     *   - OLTP_ONLY → PostgreSQL via OLTPExecutor.
     *   - OLAP_ONLY → ClickHouse via OLAPExecutor (avec FINAL optionnel).
     *   - HYBRID → exécution parallèle partie chaude (OLTP) + partie froide (OLAP),
     *              puis fusion des résultats.
     */
    public Object route(String sql) throws Exception {
        String trimmed = sql.trim().toUpperCase();

        if (trimmed.startsWith("INSERT") && trimmed.contains("SELECT")) {
            return executorFactory.getOLTPExecutor().executeRaw(sql);
        }

        QueryRouteDecision decision = analyzer.analyze(sql);

        switch (decision) {

            case OLTP_ONLY:
                return executorFactory.getOLTPExecutor().execute(sql);

            case OLAP_ONLY:
                String olapSql = sql.contains("HTAP_STRICT")
                        ? addFinalWithJSqlParser(sql)
                        : sql;
                return executorFactory.getOLAPExecutor().execute(olapSql);

            case HYBRID:
                Future<Object> fHot = pool.submit((Callable<Object>) () ->
                        executorFactory.getOLTPExecutor().execute(
                                analyzer.addHotPredicate(sql))
                );

                Future<Object> fCold = pool.submit((Callable<Object>) () ->
                        executorFactory.getOLAPExecutor().execute(
                                addFinalWithJSqlParser(
                                        analyzer.addColdPredicate(sql)))
                );
                return JDBCResultMerger.merge(fHot.get(), fCold.get());
            default:
                throw new IllegalStateException("Unexpected decision: " + decision);
        }
    }

    /**
     * Route un batch de requêtes OLTP.
     *
     * - Utilisé par l'endpoint /proxy/query/batch pour exécuter un lot d'INSERT/UPDATE.
     * - Le batching réel est réalisé dans OLTPExecutor via JDBC batch.
     */
    public void routeBatch(List<String> sqls) throws Exception {
        executorFactory.getOLTPExecutor().executeBatch(sqls);
    }

    /**
     * Arrête proprement le pool de threads utilisé pour les requêtes hybrides.
     */
    public void shutdown() {
        pool.shutdown();
    }

    /**
     * Ajoute le mot-clé FINAL sur la table principale d'un SELECT ClickHouse (ReplacingMergeTree).
     *
     * - Ne touche qu'aux requêtes SELECT simples (PlainSelect).
     * - Ne modifie rien si FINAL est déjà présent ou si la requête n'est pas compatible.
     *
     * @param sql requête SELECT d'origine
     * @return requête éventuellement réécrite avec FINAL
     */
    private String addFinalWithJSqlParser(String sql) {
        String upper = sql.toUpperCase();

        if (!upper.startsWith("SELECT")) {
            return sql;
        }

        if (upper.contains(" FINAL ")
                || upper.endsWith(" FINAL")
                || upper.contains(" SETTINGS FINAL")) {
            return sql;
        }

        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (!(stmt instanceof Select)) {
                return sql;
            }

            Select select = (Select) stmt;
            if (!(select.getSelectBody() instanceof PlainSelect)) {
                return sql;
            }

            PlainSelect ps = (PlainSelect) select.getSelectBody();
            FromItem from = ps.getFromItem();

            if (from instanceof Table) {
                Table table = (Table) from;
                String tableName = table.getFullyQualifiedName();

                String sqlUpper = sql.toUpperCase();
                String marker = " FROM " + tableName.toUpperCase();

                int idx = sqlUpper.indexOf(marker);
                if (idx >= 0) {
                    int insertPos = idx + marker.length();
                    return sql.substring(0, insertPos) + " FINAL" + sql.substring(insertPos);
                }
            }
            return sql;
        } catch (Exception e) {
            return sql;
        }
    }
}
