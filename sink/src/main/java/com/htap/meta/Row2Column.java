package com.htap.meta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Conversion générique Debezium -> Map colonne -> valeur,
 * avec support automatique des Decimal (toutes tables).
 * Ajout de _event_ts_ms pour mesurer la fraîcheur.
 */
public class Row2Column {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> convert(String jsonMessage) throws Exception {
        Map<String, Object> row = new HashMap<>();
        JsonNode root = mapper.readTree(jsonMessage);

        JsonNode payload = root.get("payload");
        if (payload == null) return row;

        JsonNode schema = root.get("schema");
        JsonNode after = payload.get("after");
        JsonNode before = payload.get("before");
        JsonNode opNode = payload.get("op");

        String op = opNode != null ? opNode.asText() : "c";

        long ts = payload.has("ts_ms") ? payload.get("ts_ms").asLong()
                : (payload.has("source") && payload.get("source").has("ts_ms"))
                ? payload.get("source").get("ts_ms").asLong()
                : System.currentTimeMillis();

        long version = "d".equals(op) ? ts + 1 : ts;

        row.put("_op", op);
        row.put("_version", version);
        row.put("_deleted", "d".equals(op) ? 1 : 0);
        // horodatage d'événement pour la fraîcheur OLAP
        row.put("_event_ts_ms", ts);

        // DELETE : on simplifie, pas de PK spécifique
        if ("d".equals(op)) {
            return row;
        }

        Map<String, Integer> decimalScales = extractDecimalScales(schema);

        JsonNode nodeToCopy = after != null ? after : before;
        if (nodeToCopy != null) {
            Iterator<String> fieldNames = nodeToCopy.fieldNames();
            while (fieldNames.hasNext()) {
                String f = fieldNames.next();
                JsonNode val = nodeToCopy.get(f);

                if (val == null || val.isNull()) {
                    row.put(f, null);
                } else if (val.isInt()) {
                    row.put(f, val.asInt());
                } else if (val.isLong()) {
                    long v = val.asLong();
                    if ("o_entry_d".equals(f)) {
                        row.put(f, v / 1000);
                    } else {
                        row.put(f, v);
                    }
                } else if (val.isBinary()) {
                    try {
                        byte[] bytes = val.binaryValue();
                        BigInteger bi = new BigInteger(bytes);
                        Integer scale = decimalScales.get(f);
                        if (scale != null) {
                            row.put(f, new BigDecimal(bi, scale));
                        } else {
                            row.put(f, new String(bytes));
                        }
                    } catch (Exception e) {
                        row.put(f, null);
                    }
                } else if (val.isTextual()) {
                    Integer scale = decimalScales.get(f);
                    if (scale != null) {
                        try {
                            byte[] bytes = Base64.getDecoder().decode(val.asText());
                            BigInteger bi = new BigInteger(bytes);
                            row.put(f, new BigDecimal(bi, scale));
                        } catch (Exception e) {
                            row.put(f, val.asText());
                        }
                    } else {
                        row.put(f, val.asText());
                    }
                } else {
                    row.put(f, val.toString());
                }
            }
        }

        return row;
    }

    /**
     * Parcourt le schema Debezium et récupère pour chaque champ Decimal son "scale".
     * Fonctionne pour toute table (users, orders, customer, etc.).
     */
    private static Map<String, Integer> extractDecimalScales(JsonNode schema) {
        Map<String, Integer> map = new HashMap<>();
        if (schema == null) return map;

        JsonNode fields = schema.get("fields");
        if (fields == null || !fields.isArray()) return map;

        for (JsonNode field : fields) {
            // on cherche le champ "after" (ou "before") qui contient la structure des colonnes
            JsonNode fieldNameNode = field.get("field");
            if (fieldNameNode == null) continue;
            String fieldName = fieldNameNode.asText();
            if (!"after".equals(fieldName) && !"before".equals(fieldName)) {
                continue;
            }
            JsonNode struct = field.get("fields");
            if (struct == null || !struct.isArray()) continue;

            for (JsonNode col : struct) {
                JsonNode colNameNode = col.get("field");
                if (colNameNode == null) continue;
                String colName = colNameNode.asText();

                JsonNode colType = col.get("type");
                JsonNode typeName = col.get("name"); // org.apache.kafka.connect.data.Decimal

                if (colType != null && "bytes".equals(colType.asText())
                        && typeName != null
                        && "org.apache.kafka.connect.data.Decimal".equals(typeName.asText())) {

                    JsonNode params = col.get("parameters");
                    if (params != null && params.has("scale")) {
                        int scale = Integer.parseInt(params.get("scale").asText());
                        map.put(colName, scale);
                    }
                }
            }
        }
        return map;
    }
}
