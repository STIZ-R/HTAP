package com.htap.meta;

import org.apache.kafka.clients.consumer.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Application de consommation Kafka vers ClickHouse.
 *
 * Optimisations fraîcheur :
 * - poll Kafka toutes les 100 ms (au lieu de 1 s)
 * - MAX_POLL_RECORDS plus petit
 * - flush time-based toutes les 1 s (inchangé)
 */
public class KafkaConsumerApp {

    // Batch max en mémoire par table avant flush forcé
    private static final int BATCH_MAX_SIZE = 30_000;       // réduit (avant 500_000)
    // Intervalle max entre deux flushs (time-based)
    private static final long FLUSH_INTERVAL_MS = 500;

    public static void main(String[] args) throws Exception {
        String kafkaBootstrap = System.getenv().getOrDefault("KAFKA_BOOTSTRAP", "kafka:9092");
        String clickhouseUrl = System.getenv().getOrDefault(
                "CLICKHOUSE_URL",
                "jdbc:clickhouse://clickhouse:8123/default?user=default&password=clickhouse&use_http_transport=true"
        );

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "sink-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 30_000);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 100 * 1024 * 1024);

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        List<String> htapsTopics = new ArrayList<>();
        for (String topic : consumer.listTopics().keySet()) {
            if (topic.startsWith("htap.")) {
                htapsTopics.add(topic);
            }
        }

        if (htapsTopics.isEmpty()) {
            System.err.println("Aucun topic htap trouvé !");
            return;
        }

//        List<String> htapTopics = Arrays.asList(
//                "htap.public.warehouse",
//                "htap.public.district",
//                "htap.public.customer",
//                "htap.public.start",
//                "htap.public.orders",
//                "htap.public.order_line"
//        );
List<String> htapTopics = Arrays.asList(
                "htap.public.customer",
                "htap.public.district",
                "htap.public.history",
                "htap.public.item",
                "htap.public.nation",
                "htap.public.new_order",
        "htap.public.oorder",
        "htap.public.order_line",
        "htap.public.region",
        "htap.public.stock",
        "htap.public.supplier",
        "htap.public.warehouse",
        "htap.public.start"
        );

        consumer.subscribe(htapTopics);
        System.out.println("Kafka consumer démarré pour les topics: " + htapTopics);

        Connection conn = null;
        while (conn == null) {
            try {
                conn = DriverManager.getConnection(clickhouseUrl);
                System.out.println("Connecté à ClickHouse !");
            } catch (SQLException e) {
                System.out.println("ClickHouse non disponible, attente 5s...");
                Thread.sleep(5000);
            }
        }

        ExecutorService insertExecutor = Executors.newFixedThreadPool(htapTopics.size() * 8);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Map<String, BatchFlusher> tableFlushers = new HashMap<>();
        for (String topic : htapTopics) {
            String table = topicToTable(topic);
            tableFlushers.put(table, new BatchFlusher(table, conn, insertExecutor));
        }

        scheduler.scheduleAtFixedRate(() -> {
            for (BatchFlusher flusher : tableFlushers.values()) {
                flusher.flush();
            }
        }, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

            for (ConsumerRecord<String, String> record : records) {
                String table = topicToTable(record.topic());
                try {
                    Map<String, Object> row = Row2Column.convert(record.value());
                    if (!row.isEmpty()) {
                        BatchFlusher flusher = tableFlushers.get(table);
                        if (flusher != null) {
                            flusher.add(row);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String topicToTable(String topic) {
        String[] parts = topic.split("\\.");
        String p = parts.length == 3 ? parts[2] : topic;
        return p.toUpperCase();
    }

    static class BatchFlusher {
        private final String table;
        private final Connection conn;
        private final ExecutorService executor;
        private final List<Map<String, Object>> batch = Collections.synchronizedList(new ArrayList<>());
        private Set<String> columnsCache = null;

        public BatchFlusher(String table, Connection conn, ExecutorService executor) {
            this.table = table;
            this.conn = conn;
            this.executor = executor;
        }

        public void add(Map<String, Object> row) {
            batch.add(row);
            if (batch.size() >= BATCH_MAX_SIZE) {
                flush();
            }
        }

        public void flush() {
            if (batch.isEmpty()) return;
            List<Map<String, Object>> toInsert;
            synchronized (batch) {
                toInsert = new ArrayList<>(batch);
                batch.clear();
            }
            executor.submit(() -> insertBatch(toInsert));
        }

        private void insertBatch(List<Map<String, Object>> batchToInsert) {
            if (batchToInsert.isEmpty()) return;
            try {
                if (columnsCache == null) {
                    columnsCache = getExistingColumns(conn, table);
                }

                List<String> colsToInsert = new ArrayList<>();
                for (String col : batchToInsert.get(0).keySet()) {
                    if (columnsCache.contains(col)) {
                        colsToInsert.add(col);
                    }
                }
                if (colsToInsert.isEmpty()) return;

                String colNames = String.join(", ", colsToInsert);
                String placeholders = String.join(", ", Collections.nCopies(colsToInsert.size(), "?"));
                String sql = "INSERT INTO " + table + " (" + colNames + ") VALUES (" + placeholders + ")";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Map<String, Object> row : batchToInsert) {
                        int idx = 1;
                        for (String col : colsToInsert) {
                            Object v = row.get(col);
                            if (v instanceof Long && isTimestampColumn(col)) {
                                // epoch millis -> java.sql.Timestamp pour ClickHouse DateTime64(3)
                                ps.setTimestamp(idx++, new java.sql.Timestamp((Long)v));
                            } else {
                                ps.setObject(idx++, v);
                            }

                        }

                        ps.addBatch();
                    }

                    ps.executeBatch();
                    System.out.println("Batch inséré dans " + table + ": " + batchToInsert.size() + " lignes");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
//
//        private boolean isTimestampColumn(String col) {
//            return col.equals("O_ENTRY_D") || col.equals("OL_DELIVERY_D") || col.equals("H_DATE") || col.equals("C_SINCE");
//        }

        private boolean isTimestampColumn(String col) {
            return col.equals("o_entry_d") || col.equals("ol_delivery_d") || col.equals("h_date") || col.equals("c_since");
        }


        private Set<String> getExistingColumns(Connection conn, String table) throws SQLException {
            Set<String> columns = new HashSet<>();
            String sql = "SELECT name FROM system.columns WHERE database = 'default' AND table = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, table);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String col = rs.getString("name");
                        columns.add(col);
                    }
                }
            }
            System.out.println("Colonnes existantes pour table " + table + ": " + columns);
            return columns;
        }
    }

}
