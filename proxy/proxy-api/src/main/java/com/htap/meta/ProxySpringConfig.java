package com.htap.meta;

import com.htap.meta.analyze.QueryAnalyzer;
import com.htap.meta.routing.QueryRouter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * Configuration Spring du proxy HTAP.
 *
 * Cette classe déclare explicitement les beans principaux :
 * - ExecutorFactory : initialise les connexions JDBC OLTP / OLAP
 * - QueryAnalyzer   : décide OLTP / OLAP / HYBRID
 * - QueryRouter     : point central de routage des requêtes
 *
 * Le but est de garder une architecture claire et testable.
 */
@Configuration
public class ProxySpringConfig {

    /*
     * Fabrique des exécutants OLTP / OLAP.
     *
     * - Lit POSTGRES_URL et CLICKHOUSE_URL depuis l’environnement
     * - Ouvre les connexions JDBC
     * - Crée OLTPExecutor et OLAPExecutor
     *
     * Bean singleton : une seule factory pour tout le proxy.
     */
    @Bean
    public ExecutorFactory executorFactory() throws Exception {
        return new ExecutorFactory();
    }

    /*
     * Analyseur SQL.
     *
     * - Analyse la requête (SELECT / INSERT / etc.)
     * - Décide si la requête va vers OLTP, OLAP ou HYBRID
     *
     * Stateless → singleton sans problème.
     */
    @Bean
    public QueryAnalyzer queryAnalyzer() {
        return new QueryAnalyzer();
    }

    /*
     * Routeur principal HTAP.
     *
     * - Utilise QueryAnalyzer pour la décision
     * - Utilise ExecutorFactory pour exécuter
     * - Ajoute FINAL pour ClickHouse
     * - Gère le parallélisme en mode HYBRID
     */
    @Bean
    public QueryRouter queryRouter(QueryAnalyzer analyzer,
                                   ExecutorFactory executorFactory) {
        return new QueryRouter(analyzer, executorFactory);
    }
}
