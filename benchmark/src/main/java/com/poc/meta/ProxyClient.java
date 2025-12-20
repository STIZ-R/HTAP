package com.poc.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class ProxyClient {

    private final String baseUrl; // ex: "http://htap-proxy:8080/proxy/query"
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final CollectionType listOfMapsType;

    public ProxyClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        MapType mapType = mapper.getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);
        this.listOfMapsType = mapper.getTypeFactory()
                .constructCollectionType(List.class, mapType);
    }

    /**
     * Exécute une requête SELECT via le proxy et renvoie la liste de lignes
     * telle que renvoyée par ProxyController (List<Map<String,Object>>).
     */
    public List<Map<String, Object>> executeSelect(String sql) throws IOException, InterruptedException {
        String body = callProxy(sql);
        // Tu as montré que pour SELECT 1, le proxy renvoie: [{"?column?":1}]
        // donc ce parsing List<Map<String,Object>> convient.
        return mapper.readValue(body, listOfMapsType);
    }

    /**
     * Exécute une requête d'écriture (INSERT/UPDATE/DELETE, DDL).
     * Si tu fais évoluer ProxyController pour renvoyer rowsAffected, tu pourras
     * parser cette info ici.
     */
    public void executeUpdate(String sql) throws IOException, InterruptedException {
        callProxy(sql);
    }

    /**
     * Envoie la requête SQL brute au proxy via GET.
     */
    private String callProxy(String sql) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(sql, StandardCharsets.UTF_8);
        URI uri = URI.create(baseUrl + "?sql=" + encoded);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("Proxy HTTP error " + resp.statusCode() + ": " + resp.body());
        }
        return resp.body();
    }
}
