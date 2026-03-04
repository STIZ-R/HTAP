package com.htap.meta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Conversion Debezium -> Map colonne->valeur pour ClickHouse DateTime64.
 * Support automatique des Decimal et timestamps ISO8601.
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
        row.put("_event_ts_ms", ts);

        // DELETE : on simplifie
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
                    row.put(normalizeColumn(f), null);
                } else if (val.isInt()) {
                    row.put(normalizeColumn(f), val.asInt());
                } else if (val.isLong()) {
                    long v = val.asLong();
                    if (isTimestampField(f)) {
                        row.put(normalizeColumn(f), v); // epoch millis
                    } else {
                        row.put(normalizeColumn(f), v);
                    }
                } else if (val.isTextual() || val.isBinary()) {
                    Integer scale = decimalScales.get(f);
                    if (scale != null) {
                        try {
                            byte[] bytes = val.isTextual() ? Base64.getDecoder().decode(val.asText()) : val.binaryValue();
                            BigInteger bi = new BigInteger(bytes);
                            row.put(normalizeColumn(f), new BigDecimal(bi, scale));
                        } catch (Exception e) {
                            row.put(normalizeColumn(f), val.isTextual() ? val.asText() : null);
                        }
                    } else if (isTimestampField(f)) {
                        // Convertit ISO8601 -> epoch ms
                        row.put(f, parseISO8601ToEpochMs(val.asText()));
                    } else {
                        row.put(normalizeColumn(f), val.isTextual() ? val.asText() : new String(val.binaryValue()));
                    }
                } else if (val.isObject()) {
                    JsonNode valueNode = val.get("value");
                    JsonNode scaleNode = val.get("scale");
                    if (valueNode != null && scaleNode != null) {
                        try {
                            byte[] bytes = Base64.getDecoder().decode(valueNode.asText());
                            BigInteger bi = new BigInteger(bytes);
                            int scale = scaleNode.asInt();
                            row.put(normalizeColumn(f), new BigDecimal(bi, scale));
                        } catch (Exception e) {
                            row.put(normalizeColumn(f), null);
                        }
                    } else {
                        row.put(normalizeColumn(f), val.toString());
                    }
                } else {
                    row.put(normalizeColumn(f), val.toString());
                }
            }
        }

        return row;
    }
    private static String normalizeColumn(String col) {
        return col.toLowerCase(); // si toutes les tables et colonnes ClickHouse sont en majuscules
    }


    private static long parseISO8601ToEpochMs(String ts) {
        Instant instant = Instant.parse(ts);
        return instant.toEpochMilli();
    }

    private static boolean isTimestampField(String field) {
        return field.equals("o_entry_d")
                || field.equals("ol_delivery_d")
                || field.equals("h_date")
                || field.equals("c_since");
    }

    private static Map<String, Integer> extractDecimalScales(JsonNode schema) {
        Map<String, Integer> map = new HashMap<>();
        if (schema == null) return map;

        JsonNode fields = schema.get("fields");
        if (fields == null || !fields.isArray()) return map;

        for (JsonNode field : fields) {
            JsonNode fieldNameNode = field.get("field");
            if (fieldNameNode == null) continue;
            String fieldName = fieldNameNode.asText();
            if (!"after".equals(fieldName) && !"before".equals(fieldName)) continue;

            JsonNode struct = field.get("fields");
            if (struct == null || !struct.isArray()) continue;

            for (JsonNode col : struct) {
                JsonNode colNameNode = col.get("field");
                if (colNameNode == null) continue;
                String colName = colNameNode.asText();

                JsonNode colType = col.get("type");
                JsonNode typeName = col.get("name");

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
