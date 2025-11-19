package com.htap.meta;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import com.clickhouse.data.*;

import java.time.Duration;
import java.util.*;

public class KafkaConsumerApp {

    public static void main(String[] args) throws Exception {

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "sink-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        // Abonner à tous les topics Debezium avec 'htap.*' par exemple
        consumer.subscribe(Collections.singletonList("htap.public.users"));

        System.out.println("Kafka consumer démarré...");

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

            for (ConsumerRecord<String, String> record : records) {
                try {
                    Map<String, Object> columns = Row2Column.convert(record.value());
                    System.out.println(columns);

                    // TODO: insérer dans ClickHouse
                    // insertClickHouse(columns);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
