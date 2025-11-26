package com.htap.meta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public class Row2Column {
    private static final ObjectMapper mapper = new ObjectMapper();

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
            version = ts + 1; // delete > insert/update
        }

        row.put("_op", op);
        row.put("_version", version);
        row.put("_deleted", "d".equals(op) ? 1 : 0);

        // ---- CAS DELETE ----
        if ("d".equals(op)) {
            if (before != null && before.has("id")) {
                row.put("id", before.get("id").asLong());
            }
            return row;  // Delete = PK seule + deleted flag
        }

        // ---- INSERT / UPDATE ----
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
