package com.htap.meta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class Row2Column {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Transforme un message Debezium en colonnes
     * @param jsonMessage le message Kafka Debezium
     * @return map colonne -> valeur
     * @throws Exception en cas d'erreur JSON
     */
    public static Map<String, Object> convert(String jsonMessage) throws Exception {
        Map<String, Object> row = new HashMap<>();
        JsonNode root = mapper.readTree(jsonMessage);
        JsonNode payload = root.get("payload");

        if (payload == null) {
            return row;
        }

        // 'after' contient la ligne insérée ou mise à jour
        JsonNode after = payload.get("after");
        JsonNode before = payload.get("before");
        JsonNode op = payload.get("op"); // c = insert, u = update, d = delete, r = snapshot

        if (after != null) {
            after.fieldNames().forEachRemaining(field -> {
                row.put(field, after.get(field).isNull() ? null : after.get(field).asText());
            });
            row.put("_op", op.asText());
        } else if (before != null) { // pour les deletes
            before.fieldNames().forEachRemaining(field -> {
                row.put(field, before.get(field).isNull() ? null : before.get(field).asText());
            });
            row.put("_op", op.asText());
        }

        return row;
    }
}
