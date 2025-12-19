package com.htap.meta;

import com.htap.meta.routing.QueryRouter;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST exposant le proxy HTAP.
 *
 * Fournit des endpoints HTTP permettant d'envoyer une requête SQL
 * au proxy, qui la route ensuite vers PostgreSQL ou ClickHouse.
 */
@RestController
@RequestMapping("/proxy")
public class ProxyController {

    private final QueryRouter queryRouter;

    /**
     * Injection du QueryRouter existant.
     *
     * @param queryRouter composant de routage HTAP
     */
    public ProxyController(QueryRouter queryRouter) {
        this.queryRouter = queryRouter;
    }

    /**
     * Endpoint GET simple :
     * /proxy/query?sql=SELECT+*+FROM+orders
     *
     * @param sql requête SQL brute
     * @return résultat renvoyé par le QueryRouter (liste de lignes, null, etc.)
     * @throws Exception si l'exécution de la requête échoue
     */
    @GetMapping("/query")
    public Object executeQuery(@RequestParam("sql") String sql) throws Exception {
        return queryRouter.route(sql);
    }

    /**
     * Payload pour l'appel POST JSON.
     */
    public static class SqlRequest {
        public String sql;
    }

    /**
     * Endpoint POST JSON :
     * POST /proxy/query
     * { "sql": "SELECT * FROM orders" }
     *
     * @param req objet contenant la requête SQL
     * @return résultat renvoyé par le QueryRouter
     * @throws Exception si l'exécution de la requête échoue
     */
    @PostMapping("/query")
    public Object executeQueryPost(@RequestBody SqlRequest req) throws Exception {
        return queryRouter.route(req.sql);
    }
}
