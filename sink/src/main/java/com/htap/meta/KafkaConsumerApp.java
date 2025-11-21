package com.htap.meta;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.PartitionInfo;

import java.sql.*;
import java.time.Duration;
import java.util.*;

public class KafkaConsumerApp {

    private static final int BATCH_SIZE = 1000;

    public static void main(String[] args) throws Exception {

        String kafkaBootstrap = System.getenv().getOrDefault("KAFKA_BOOTSTRAP", "kafka:9092");
        String clickhouseUrl = System.getenv().getOrDefault(
                "CLICKHOUSE_URL",
                "jdbc:clickhouse://clickhouse:8123/default?user=default&password=clickhouse&use_http_transport=true"
        );

        System.out.println("ClickHouse URL: " + clickhouseUrl);

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "sink-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        // Lister tous les topics qui commencent par htap.
        Map<String, List<PartitionInfo>> allTopics = consumer.listTopics();
        List<String> htapTopics = new ArrayList<>();
        for (String topic : allTopics.keySet()) {
            if (topic.startsWith("htap.")) {
                htapTopics.add(topic);
            }
        }

        if (htapTopics.isEmpty()) {
            System.err.println("Aucun topic htap trouvé, arrêt du consumer.");
            return;
        }

        consumer.subscribe(htapTopics);
        System.out.println("Kafka consumer démarré pour les topics: " + htapTopics);

        // Connexion ClickHouse
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

        Map<String, List<Map<String, Object>>> batches = new HashMap<>();

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

            for (ConsumerRecord<String, String> record : records) {
                try {
                    String table = topicToTable(record.topic());
                    Map<String, Object> row = Row2Column.convert(record.value());

                    // Supprimer uniquement les champs "before" et "after", garder _version et _deleted
                    row.keySet().removeIf(k -> k.equals("before") || k.equals("after"));

                    batches.computeIfAbsent(table, k -> new ArrayList<>()).add(row);

                    // Insert batch si taille dépassée
                    if (batches.get(table).size() >= BATCH_SIZE) {
                        insertBatch(conn, table, batches.get(table));
                        batches.get(table).clear();
                    }
                } catch (Exception e) {
                    System.err.println("Erreur traitement record: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Flush restant
            for (Map.Entry<String, List<Map<String, Object>>> entry : batches.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    try {
                        insertBatch(conn, entry.getKey(), entry.getValue());
                        entry.getValue().clear();
                    } catch (Exception e) {
                        System.err.println("Erreur insertion batch pour table " + entry.getKey() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static String topicToTable(String topic) {
        String[] parts = topic.split("\\.");
        return parts.length == 3 ? parts[2] : topic;
    }

    private static void insertBatch(Connection conn, String table, List<Map<String, Object>> batch) throws SQLException {
        if (batch.isEmpty()) return;

        // Récupérer les colonnes existantes dans ClickHouse
        Set<String> existingColumns = getExistingColumns(conn, table);

        List<String> columnsToInsert = new ArrayList<>();
        for (String col : batch.get(0).keySet()) {
            if (existingColumns.contains(col)) {
                columnsToInsert.add(col);
            }
        }

        if (columnsToInsert.isEmpty()) {
            System.err.println("Aucune colonne correspondante trouvée dans " + table);
            return;
        }

        String colNames = String.join(", ", columnsToInsert);
        String placeholders = String.join(", ", Collections.nCopies(columnsToInsert.size(), "?"));
        String sql = "INSERT INTO " + table + " (" + colNames + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map<String, Object> row : batch) {
                int idx = 1;
                for (String col : columnsToInsert) {
                    ps.setObject(idx++, row.get(col));
                }
                ps.addBatch();
            }
            ps.executeBatch();
            System.out.println("Batch inséré dans " + table + ": " + batch.size() + " lignes");
        }
    }

    private static Set<String> getExistingColumns(Connection conn, String table) throws SQLException {
        Set<String> columns = new HashSet<>();
        String sql = "SELECT name FROM system.columns WHERE database = 'default' AND table = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    columns.add(rs.getString("name"));
                }
            }
        }
        return columns;
    }
}
