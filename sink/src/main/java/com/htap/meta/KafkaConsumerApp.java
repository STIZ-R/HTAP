package com.htap.meta;

import org.apache.kafka.clients.consumer.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;

public class KafkaConsumerApp {

    private static final int BATCH_SIZE = 1000;

    public static void main(String[] args) throws Exception {

        String kafkaBootstrap = System.getenv().getOrDefault("KAFKA_BOOTSTRAP", "kafka:9092");
        String clickhouseUrl = System.getenv().getOrDefault("CLICKHOUSE_URL", "jdbc:ch://clickhouse:8123/default?use_http_transport=true");
        System.out.println("ClickHouse URL: " + clickhouseUrl);

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "sink-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("htap.public.users"));

        System.out.println("Kafka consumer démarré...");

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

        List<Map<String, Object>> batch = new ArrayList<>();

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

            for (ConsumerRecord<String, String> record : records) {
                try {
                    Map<String, Object> row = Row2Column.convert(record.value());
                    batch.add(row);

                    if (batch.size() >= BATCH_SIZE) {
                        insertBatch(conn, batch);
                        batch.clear();
                    }
                } catch (Exception e) {
                    System.err.println("Erreur traitement record: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            if (!batch.isEmpty()) {
                try {
                    insertBatch(conn, batch);
                    batch.clear();
                } catch (Exception e) {
                    System.err.println("Erreur insertion batch: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private static void insertBatch(Connection conn, List<Map<String, Object>> batch) throws SQLException {
        String sql = "INSERT INTO users (id, nom, email) VALUES (?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map<String, Object> row : batch) {
                ps.setObject(1, row.get("id"));
                ps.setObject(2, row.get("nom"));
                ps.setObject(3, row.get("email"));
                ps.addBatch();
            }
            ps.executeBatch();
            System.out.println("Batch inséré: " + batch.size() + " lignes");
        }
    }
}
