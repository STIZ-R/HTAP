package com.poc.meta;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Client HTTP du proxy HTAP
 *
 * - Permet d'envoyer des requêtes SQL uniques ou en batch
 * - Connexion persistante HTTP/1.1 pour la performance
 */
public class ProxyClient {

    private final String baseUrl;        // URL du proxy (ex: http://localhost:8080/proxy/query)
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public ProxyClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /*
     * Envoi d'un SQL unique (OLAP ou debug)
     */
    public void executeSingle(String sql) throws Exception {
        Map<String, String> payload = Map.of("sql", sql);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .timeout(Duration.ofSeconds(10))
                .build();

        httpClient.send(req, HttpResponse.BodyHandlers.discarding());
    }

    /*
     * Envoi d'un batch de requêtes OLTP
     *
     * Le batch est envoyé sur /proxy/query/batch avec JSON:
     * { "statements": [ "INSERT ...", "INSERT ..." ] }
     */
    public void executeBatch(List<String> statements) throws Exception {

        Map<String, Object> payload = new HashMap<>();
        payload.put("statements", statements);

        String jsonBody = mapper.writeValueAsString(payload);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/batch"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() >= 400) {
            throw new RuntimeException(resp.body());
        }
    }
}
