package com.htap.meta;

import com.htap.meta.routing.QueryRouter;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST du proxy HTAP.
 *
 * Ce contrôleur expose une API HTTP volontairement simple :
 * - GET  /proxy/query        → compatibilité, debug
 * - POST /proxy/query        → requête SQL unique
 * - POST /proxy/query/batch  → batch de requêtes (PERFORMANCE)
 *
 * IMPORTANT :
 * - Toute la logique métier est dans QueryRouter
 * - Le contrôleur ne fait QUE du transport HTTP
 */
@RestController
@RequestMapping("/proxy")
public class ProxyController {

    private final QueryRouter queryRouter;

    /**
     * Injection du QueryRouter (Spring).
     */
    public ProxyController(QueryRouter queryRouter) {
        this.queryRouter = queryRouter;
    }

    /**
     *
     * GET /proxy/query?sql=SELECT+1
     *
     * À utiliser pour :
     * - tests manuels
     * - healthcheck
     * - debug
     *
     */
    @GetMapping("/query")
    public Object executeQuery(@RequestParam("sql") String sql) throws Exception {
        return queryRouter.route(sql);
    }



    /**
     *
     * POST /proxy/query
     * {
     *   "sql": "SELECT * FROM orders"
     * }
     *
     * Utile pour :
     * - clients simples
     * - compatibilité
     */
    @PostMapping("/query")
    public Object executeQueryPost(@RequestBody SqlRequest req) throws Exception {
        return queryRouter.route(req.sql);
    }

    /**
     *
     * POST /proxy/query/batch
     *
     * {
     *   "statements": [
     *     "INSERT INTO orders ...",
     *     "INSERT INTO orders ...",
     *     "INSERT INTO orders ..."
     *   ]
     * }
     *
     * Ce endpoint est CRITIQUE pour atteindre > 1000 TPS :
     * - 1 appel HTTP = N requêtes SQL
     * - Le router s’occupe du dispatch OLTP / OLAP
     */
    @PostMapping("/query/batch")
    public void executeBatch(@RequestBody SqlBatchRequest req) throws Exception {
        queryRouter.routeBatch(req.statements);
    }


    /*
     * ==========================
     * DTOs HTTP
     * ==========================
     */

    /**
     * Payload pour SQL unique.
     */
    public static class SqlRequest {
        public String sql;
    }

    /**
     * Payload pour batch SQL.
     */
    public static class SqlBatchRequest {
        public List<String> statements;
    }
}
