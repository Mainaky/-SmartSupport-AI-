package com.smartsupport.config;

import com.smartsupport.kafka.TicketEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AppConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // ─── Kafka Producer ────────────────────────────────────────────────────────

    @Bean
    public ProducerFactory<String, TicketEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put("spring.json.add.type.headers", false); // Consumers use their own compatible event DTO
        config.put(ProducerConfig.ACKS_CONFIG, "all");           // strongest durability
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // exactly-once
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, TicketEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ─── Kafka Topics (auto-created on startup) ────────────────────────────────

    @Bean
    public NewTopic ticketEventsTopic() {
        return TopicBuilder.name("ticket-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic criticalAlertsTopic() {
        return TopicBuilder.name("critical-ticket-alerts")
                .partitions(1)
                .replicas(1)
                .build();
    }

    // ─── REST Template (for AI service calls) ─────────────────────────────────

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
