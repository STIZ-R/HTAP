package com.poc.meta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client HTTP du proxy HTAP.
 *
 * - Permet d'envoyer des requêtes SQL uniques ou en batch.
 * - Fournit quelques helpers pour récupérer des scalaires.
 */
public class ProxyClient implements ProxyClientInterface{

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public ProxyClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /** Envoi d'un SQL unique, réponse ignorée. */
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

    /** Envoi d'un batch de requêtes OLTP (liste de statements). */
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

    /**
     * Envoie un SELECT qui renvoie un scalaire INT (par ex. count(*)).
     * Suppose que le proxy renvoie un JSON avec soit une liste de lignes,
     * soit un objet avec une colonne unique.
     */
    public int executeScalarInt(String sql) throws Exception {
        Map<String, String> payload = Map.of("sql", sql);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException(resp.body());
        }

        JsonNode root = mapper.readTree(resp.body());
        if (root.isArray() && root.size() > 0) {
            JsonNode first = root.get(0);
            return first.fields().next().getValue().asInt();
        }
        return 0;
    }

    /**
     * Envoie un SELECT qui renvoie un timestamp max, et retourne en millis.
     * À adapter en fonction du format exact que ton proxy renvoie.
     */
    public Long executeScalarTimestampMillis(String sql) throws Exception {
        Map<String, String> payload = Map.of("sql", sql);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException(resp.body());
        }

        JsonNode root = mapper.readTree(resp.body());
        if (!root.isArray() || root.size() == 0) {
            return null;
        }
        JsonNode first = root.get(0);
        JsonNode valueNode = first.fields().next().getValue();
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }

        if (valueNode.isNumber()) {
            long v = valueNode.asLong();
            long lastTsMillis = (v < 1_000_000_000_000L) ? v * 1000 : v;
            return lastTsMillis;
        }

        String ts = valueNode.asText();
        OffsetDateTime odt = OffsetDateTime.parse(ts);
        return odt.toInstant().toEpochMilli();
    }

}
