package com.htap.meta;

import org.apache.kafka.clients.consumer.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class KafkaConsumerApp {

    private static final int BATCH_MAX_SIZE = 500_000;
    private static final long FLUSH_INTERVAL_MS = 2000; // flush toutes les 2s

    public static void main(String[] args) throws Exception {
        String kafkaBootstrap = System.getenv().getOrDefault("KAFKA_BOOTSTRAP", "kafka:9092");
        String clickhouseUrl = System.getenv().getOrDefault(
                "CLICKHOUSE_URL",
                "jdbc:clickhouse://clickhouse:8123/default?user=default&password=clickhouse&use_http_transport=true"
        );

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "sink-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, BATCH_MAX_SIZE);

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        List<String> htapTopics = new ArrayList<>();
        for (String topic : consumer.listTopics().keySet()) {
            if (topic.startsWith("htap.")) htapTopics.add(topic);
        }

        if (htapTopics.isEmpty()) {
            System.err.println("Aucun topic htap trouvé !");
            return;
        }
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

        ExecutorService insertExecutor = Executors.newFixedThreadPool(htapTopics.size() * 2);
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
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

            for (ConsumerRecord<String, String> record : records) {
                String table = topicToTable(record.topic());
                Map<String, Object> row = Row2Column.convert(record.value());

                // Ignorer les lignes invalides
                Object idObj = row.get("id");
                if (idObj == null) continue;

                long id;
                try {
                    id = Long.parseLong(idObj.toString());
                } catch (Exception e) {
                    continue; // id illisible → on skip
                }

                if (id == 0) continue;

                tableFlushers.get(table).add(row);
            }

            consumer.commitSync();
        }
    }

    private static String topicToTable(String topic) {
        String[] parts = topic.split("\\.");
        return parts.length == 3 ? parts[2] : topic;
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
            if (batch.size() >= BATCH_MAX_SIZE) flush();
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
                if (columnsCache == null) columnsCache = getExistingColumns(conn, table);

                List<String> colsToInsert = new ArrayList<>();
                for (String col : batchToInsert.get(0).keySet()) if (columnsCache.contains(col)) colsToInsert.add(col);
                if (colsToInsert.isEmpty()) return;

                String colNames = String.join(", ", colsToInsert);
                String placeholders = String.join(", ", Collections.nCopies(colsToInsert.size(), "?"));
                String sql = "INSERT INTO " + table + " (" + colNames + ") VALUES (" + placeholders + ")";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Map<String, Object> row : batchToInsert) {
                        int idx = 1;
                        for (String col : colsToInsert) ps.setObject(idx++, row.get(col));
                        ps.addBatch();

                        if ("1".equals(row.get("_deleted").toString()) || (row.get("_deleted") instanceof Integer && (Integer) row.get("_deleted") == 1)) {
                            System.out.println("DELETE appliqué id=" + row.get("id") + " _version=" + row.get("_version"));
                        }
                    }
                    ps.executeBatch();
                    System.out.println("Batch inséré dans " + table + ": " + batchToInsert.size() + " lignes");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private Set<String> getExistingColumns(Connection conn, String table) throws SQLException {
            Set<String> columns = new HashSet<>();
            String sql = "SELECT name FROM system.columns WHERE database = 'default' AND table = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, table);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) columns.add(rs.getString("name"));
                }
            }
            return columns;
        }
    }

}



//package com.htap.meta;
//
//import org.apache.kafka.clients.consumer.*;
//import org.apache.kafka.common.TopicPartition;
//
//import java.sql.*;
//import java.time.Duration;
//import java.util.*;
//import java.util.concurrent.*;
//
//public class KafkaConsumerApp {
//
//
//    private static final int BATCH_MAX_SIZE = 50_000;   // batch idéal pour ClickHouse
//    private static final long FLUSH_INTERVAL_MS = 2000; // flush régulier
//    private static final int THREADS_PER_TOPIC = 3;
//
//    public static void main(String[] args) throws Exception {
//        String kafkaBootstrap = System.getenv().getOrDefault("KAFKA_BOOTSTRAP", "kafka:9092");
//        String clickhouseUrl = System.getenv().getOrDefault(
//                "CLICKHOUSE_URL",
//                "jdbc:clickhouse://clickhouse:8123/default?user=default&password=clickhouse&use_http_transport=true"
//        );
//
//        Properties props = new Properties();
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrap);
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, "sink-group");
//        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
//        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
//        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
//
//        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
//
//        List<String> htapTopics;
//        while (true) {
//            htapTopics = new ArrayList<>();
//            for (String topic : consumer.listTopics().keySet()) if (topic.startsWith("htap.")) htapTopics.add(topic);
//            if (!htapTopics.isEmpty()) break;
//            System.out.println("Aucun topic htap trouvé, nouvelle tentative dans 5s...");
//            Thread.sleep(5000);
//        }
//        System.out.println("Topics htap trouvés: " + htapTopics);
//        consumer.subscribe(htapTopics);
//
//        ExecutorService executor = Executors.newFixedThreadPool(htapTopics.size() * THREADS_PER_TOPIC);
//
//        // Cache des colonnes par table
//        Map<String, Set<String>> tableColumnsCache = new ConcurrentHashMap<>();
//
//        while (true) {
//            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
//            Map<TopicPartition, List<ConsumerRecord<String,String>>> partitionRecords = new HashMap<>();
//
//            for (ConsumerRecord<String,String> record : records) {
//                partitionRecords.computeIfAbsent(new TopicPartition(record.topic(), record.partition()), k -> new ArrayList<>()).add(record);
//            }
//
//            for (Map.Entry<TopicPartition, List<ConsumerRecord<String,String>>> entry : partitionRecords.entrySet()) {
//                executor.submit(() -> {
//                    try (Connection conn = DriverManager.getConnection(clickhouseUrl)) {
//                        processPartitionBatch(conn, entry.getValue(), tableColumnsCache);
//                    } catch (SQLException e) {
//                        e.printStackTrace();
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//                });
//            }
//
//            consumer.commitSync();
//        }
//    }
//
//    private static void processPartitionBatch(Connection conn, List<ConsumerRecord<String,String>> records, Map<String, Set<String>> cache) throws Exception {
//        Map<String, List<Map<String,Object>>> batches = new HashMap<>();
//        Map<String, Long> lastFlush = new HashMap<>();
//        long now = System.currentTimeMillis();
//
//        for (ConsumerRecord<String,String> record : records) {
//            String table = topicToTable(record.topic());
//            Map<String,Object> row = Row2Column.convert(record.value());
//            row.keySet().removeIf(k -> k.equals("before") || k.equals("after"));
//
//            batches.computeIfAbsent(table, k -> new ArrayList<>()).add(row);
//            lastFlush.putIfAbsent(table, now);
//
//            if (batches.get(table).size() >= BATCH_MAX_SIZE) {
//                insertBatch(conn, table, batches.get(table), cache);
//                batches.get(table).clear();
//                lastFlush.put(table, now);
//            }
//        }
//
//        now = System.currentTimeMillis();
//        for (String table : batches.keySet()) {
//            List<Map<String,Object>> batch = batches.get(table);
//            if (!batch.isEmpty() && (batch.size() >= 1 || now - lastFlush.get(table) >= FLUSH_INTERVAL_MS)) {
//                insertBatch(conn, table, batch, cache);
//                batch.clear();
//                lastFlush.put(table, now);
//            }
//        }
//    }
//
//    private static String topicToTable(String topic) {
//        String[] parts = topic.split("\\.");
//        return parts.length == 3 ? parts[2] : topic;
//    }
//
//    private static void insertBatch(Connection conn, String table, List<Map<String,Object>> batch, Map<String, Set<String>> cache) {
//        if (batch.isEmpty()) return;
//
//        try {
//            Set<String> columns = cache.computeIfAbsent(table, t -> {
//                try {
//                    return getExistingColumns(conn, t);
//                } catch (SQLException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//
//            List<String> colsToInsert = new ArrayList<>();
//            for (String col : batch.get(0).keySet()) if (columns.contains(col)) colsToInsert.add(col);
//            if (colsToInsert.isEmpty()) return;
//
//            String colNames = String.join(", ", colsToInsert);
//            String placeholders = String.join(", ", Collections.nCopies(colsToInsert.size(), "?"));
//            String sql = "INSERT INTO " + table + " (" + colNames + ") VALUES (" + placeholders + ")";
//
//            try (PreparedStatement ps = conn.prepareStatement(sql)) {
//                for (Map<String,Object> row : batch) {
//                    int idx = 1;
//                    for (String col : colsToInsert) ps.setObject(idx++, row.get(col));
//                    ps.addBatch();
//                }
//                ps.executeBatch();
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static Set<String> getExistingColumns(Connection conn, String table) throws SQLException {
//        Set<String> cols = new HashSet<>();
//        String sql = "SELECT name FROM system.columns WHERE database = 'default' AND table = ?";
//        try (PreparedStatement ps = conn.prepareStatement(sql)) {
//            ps.setString(1, table);
//            try (ResultSet rs = ps.executeQuery()) {
//                while (rs.next()) cols.add(rs.getString("name"));
//            }
//        }
//        return cols;
//    }
//
//
//}




