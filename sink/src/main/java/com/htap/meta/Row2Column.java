package com.htap.meta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * Conversion des messages Debezium (JSON) en ligne générique.
 *
 * - Parse le JSON produit par Debezium (structure payload/before/after/op).
 * - Extrait l'opération (c, u, d), le timestamp logique ts_ms et en déduit un _version.
 * - Produit une Map<String, Object> avec :
 *   - des méta-colonnes (_op, _version, _deleted),
 *   - les colonnes métier (id, etc.) copiées depuis "after" ou "before".
 */
public class Row2Column {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Convertit un message JSON Debezium en map colonne → valeur.
     *
     * @param jsonMessage message JSON brut reçu depuis Kafka
     * @return une Map représentant la ligne à insérer (peut être vide si payload null)
     * @throws Exception si le parsing JSON échoue
     */
    public static Map<String, Object> convert(String jsonMessage) throws Exception {
        Map<String, Object> row = new HashMap<>();
        JsonNode root = mapper.readTree(jsonMessage);
        JsonNode payload = root.get("payload");
        if (payload == null) return row;

        JsonNode after = payload.get("after");
        JsonNode before = payload.get("before");
        JsonNode opNode = payload.get("op");

        String op = opNode != null ? opNode.asText() : "c";

        long ts = payload.has("ts_ms") ? payload.get("ts_ms").asLong()
                : (payload.has("source") && payload.get("source").has("ts_ms"))
                ? payload.get("source").get("ts_ms").asLong()
                : System.currentTimeMillis();

        long version = ts;

        if ("d".equals(op)) {
            version = ts + 1; // delete > insert/update; we give it the priority for the merge on _version
        }

        row.put("_op", op);
        row.put("_version", version);
        row.put("_deleted", "d".equals(op) ? 1 : 0);

        // DELETE
        if ("d".equals(op)) {
            if (before != null && before.has("id")) {
                row.put("id", before.get("id").asLong());
            }
            return row;  // Delete = PK seule + deleted flag
        }

        // INSERT / UPDATE
        JsonNode nodeToCopy = after != null ? after : before;
        if (nodeToCopy != null) {
            nodeToCopy.fieldNames().forEachRemaining(f -> {
                JsonNode val = nodeToCopy.get(f);
                if (val.isInt()) row.put(f, val.asInt());
                else if (val.isLong()) row.put(f, val.asLong());
                else row.put(f, val.isNull() ? null : val.asText());
            });
        }

        return row;
    }

}
