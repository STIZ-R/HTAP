package com.htap.meta;

import org.apache.kafka.clients.consumer.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Application de consommation Kafka vers ClickHouse.
 *
 * - Découvre tous les topics Kafka dont le nom commence par "htap.".
 * - Consomme les messages JSON (Debezium) sur ces topics.
 * - Convertit chaque message en Map colonne → valeur (Row2Column).
 * - Regroupe les lignes par table cible et les insère en batch dans ClickHouse.
 */
public class KafkaConsumerApp {

    private static final int BATCH_MAX_SIZE = 500_000;
    private static final long FLUSH_INTERVAL_MS = 2000; // flush toutes les 2s (si le batch_size n'est pas atteint)

    /**
     * Point d'entrée principal.
     *
     * - Configure le consumer Kafka (bootstrap, group, désérialisations, etc.).
     * - Liste les topics et s'abonne à ceux commençant par "htap.".
     * - Établit une connexion JDBC vers ClickHouse (avec retry).
     * - Crée un BatchFlusher par table cible.
     * - Boucle de poll Kafka, convertit chaque record en ligne et l'envoie
     *   au BatchFlusher correspondant qui gère l'insertion en batch.
     */
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

                Object idObj = row.get("id");
                if (idObj == null) continue;

                long id;
                try {
                    id = Long.parseLong(idObj.toString());
                } catch (Exception e) {
                    continue;
                }

                if (id == 0) continue;

                tableFlushers.get(table).add(row);
            }

            consumer.commitSync();
        }
    }

    /**
     * Déduit le nom de la table ClickHouse à partir du nom du topic Kafka.
     * Exemple : "htap.public.orders" → "orders".
     *
     * @param topic nom du topic Kafka
     * @return nom de la table cible
     */
    private static String topicToTable(String topic) {
        String[] parts = topic.split("\\.");
        return parts.length == 3 ? parts[2] : topic;
    }

    /**
     * Gestionnaire de batch pour une table donnée.
     *
     * - Accumule les lignes dans une liste synchronisée.
     * - Déclenche un flush soit quand la taille max est atteinte,
     *   soit via le scheduler périodique.
     * - À chaque flush, soumet un travail d'insertion batch au pool de threads.
     */
    static class BatchFlusher {
        private final String table;
        private final Connection conn;
        private final ExecutorService executor;
        private final List<Map<String, Object>> batch = Collections.synchronizedList(new ArrayList<>());
        private Set<String> columnsCache = null;

        /**
         * @param table    nom de la table cible dans ClickHouse
         * @param conn     connexion JDBC vers ClickHouse
         * @param executor pool de threads pour exécuter les insertions en parallèle
         */
        public BatchFlusher(String table, Connection conn, ExecutorService executor) {
            this.table = table;
            this.conn = conn;
            this.executor = executor;
        }

        /**
         * Ajoute une ligne au batch en mémoire.
         * Si la taille max du batch est atteinte, déclenche un flush immédiat.
         *
         * @param row ligne représentée par un Map colonne → valeur
         */
        public void add(Map<String, Object> row) {
            batch.add(row);
            if (batch.size() >= BATCH_MAX_SIZE) flush();
        }

        /**
         * Déclenche l'insertion asynchrone du batch courant.
         * Si le batch est vide, ne fait rien.
         */
        public void flush() {
            if (batch.isEmpty()) return;
            List<Map<String, Object>> toInsert;
            synchronized (batch) {
                toInsert = new ArrayList<>(batch);
                batch.clear();
            }
            executor.submit(() -> insertBatch(toInsert));
        }

        /**
         * Insère un batch de lignes dans ClickHouse via un INSERT préparé.
         *
         * - Récupère et met en cache la liste des colonnes existantes pour la table.
         * - Filtre les colonnes à insérer en fonction de ce schéma.
         * - Construit un INSERT avec placeholders et exécute un batch JDBC.
         *
         * @param batchToInsert liste de lignes à insérer
         */
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
                    }
                    ps.executeBatch();
                    System.out.println("Batch inséré dans " + table + ": " + batchToInsert.size() + " lignes");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        /**
         * Récupère la liste des colonnes existantes pour une table ClickHouse
         * en interrogeant system.columns.
         *
         * @param conn  connexion JDBC vers ClickHouse
         * @param table nom de la table
         * @return ensemble des noms de colonnes
         * @throws SQLException si la requête sur system.columns échoue
         */
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

