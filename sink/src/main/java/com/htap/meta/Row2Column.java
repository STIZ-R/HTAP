package com.htap.meta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class Row2Column {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Transforme un message Debezium en colonnes pour ClickHouse
     * @param jsonMessage le message Kafka Debezium
     * @return map colonne -> valeur
     * @throws Exception en cas d'erreur JSON
     */
    public static Map<String, Object> convert(String jsonMessage) throws Exception {
        Map<String, Object> row = new HashMap<>();
        JsonNode root = mapper.readTree(jsonMessage);
        JsonNode payload = root.get("payload");
        if (payload == null) return row;

        JsonNode after = payload.get("after");
        JsonNode before = payload.get("before");
        JsonNode op = payload.get("op"); // c = insert, u = update, d = delete

        // Récupérer le timestamp unique pour _version
        long version = payload.has("ts_ms") ? payload.get("ts_ms").asLong() :
                (payload.has("source") && payload.get("source").has("ts_ms")) ?
                        payload.get("source").get("ts_ms").asLong() :
                        System.currentTimeMillis();

        // Copier les colonnes
        if (after != null) {
            after.fieldNames().forEachRemaining(f -> row.put(f, after.get(f).isNull() ? null : after.get(f).asText()));
        } else if (before != null) { // delete
            before.fieldNames().forEachRemaining(f -> row.put(f, before.get(f).isNull() ? null : before.get(f).asText()));
        }

        row.put("_op", op != null ? op.asText() : "c");
        row.put("_version", version);
        row.put("_deleted", "d".equals(op != null ? op.asText() : "") ? 1 : 0);

        return row;
    }
}
