package com.htap.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final java.net.http.HttpClient CLIENT =
            java.net.http.HttpClient.newBuilder()
                    .version(java.net.http.HttpClient.Version.HTTP_1_1)
                    .build();

    /**
     * Transforme endpoint="htap-proxy:8080/htapdb" → "http://htap-proxy:8080"
     * + path → "http://htap-proxy:8080/proxy/query"
     */
    private static URI buildUri(String endpoint, String path) {
        String base = endpoint;

        // Enlève jdbc:htap:// et garde "htap-proxy:8080/htapdb"
        if (base.startsWith("jdbc:htap://")) {
            base = base.substring(11);
        }

        // Enlève /htapdb pour ne garder que host:port
        int slash = base.indexOf('/');
        if (slash > 0) {
            base = base.substring(0, slash);
        }

        // Ajoute http://
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            base = "http://" + base;
        }

        // Path avec / au début
        String p = path.startsWith("/") ? path : "/" + path;

        return URI.create(base + p);
    }

    @SuppressWarnings("unchecked")
    public static Object post(String base, String path, String sql) {
        try {
            Map<String, Object> payload = Map.of("sql", sql);
            String body = MAPPER.writeValueAsString(payload);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(buildUri(base, path))  // ✅ CORRIGÉ ICI
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp =
                    CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() >= 400) {
                throw new RuntimeException("Proxy error: " + resp.body());
            }

            if (resp.body() == null || resp.body().isEmpty()) {
                return null;
            }

            return MAPPER.readValue(resp.body(), Object.class);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void postBatch(String base, List<String> sqls) {
        try {
            Map<String, Object> payload = Map.of("statements", sqls);
            String body = MAPPER.writeValueAsString(payload);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(buildUri(base, "/proxy/query/batch"))  // ✅ CORRIGÉ ICI AUSSI
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp =
                    CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() >= 400) {
                throw new RuntimeException("Batch error: " + resp.body());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}