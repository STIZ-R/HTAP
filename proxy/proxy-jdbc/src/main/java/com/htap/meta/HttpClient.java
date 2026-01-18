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

    @SuppressWarnings("unchecked")
    public static Object post(String base, String path, String sql) {
        try {
            Map<String, Object> payload = Map.of("sql", sql);
            String body = MAPPER.writeValueAsString(payload);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(base + path))
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
                    .uri(URI.create(base + "/proxy/query/batch"))
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
