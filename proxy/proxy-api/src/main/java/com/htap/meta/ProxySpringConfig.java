package com.htap.meta;

import com.htap.meta.ExecutorFactory;
import com.htap.meta.analyze.QueryAnalyzer;
import com.htap.meta.routing.QueryRouter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Spring qui expose les composants du proxy HTAP
 * (ExecutorFactory, QueryAnalyzer, QueryRouter) en tant que beans.
 */
@Configuration
public class ProxySpringConfig {

    /**
     * Bean qui encapsule la création des exécutants OLTP/OLAP
     * et les connexions JDBC associées (PostgreSQL, ClickHouse).
     */
    @Bean
    public ExecutorFactory executorFactory() throws Exception {
        return new ExecutorFactory();
    }

    /**
     * Bean responsable de l'analyse syntaxique des requêtes SQL
     * et de la décision OLTP/OLAP/HYBRID.
     */
    @Bean
    public QueryAnalyzer queryAnalyzer() {
        return new QueryAnalyzer();
    }

    /**
     * Bean principal de routage qui utilise l'analyseur
     * et la fabrique d'exécuteurs pour exécuter les requêtes.
     */
    @Bean
    public QueryRouter queryRouter(QueryAnalyzer analyzer,
                                   ExecutorFactory executorFactory) {
        return new QueryRouter(analyzer, executorFactory);
    }
}
