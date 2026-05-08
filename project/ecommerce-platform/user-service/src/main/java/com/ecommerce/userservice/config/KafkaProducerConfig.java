package com.ecommerce.userservice.config;

import com.ecommerce.common.dto.audit.AuditLogDTO;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, AuditLogDTO> auditProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Durability: wait for all replicas
        config.put(ProducerConfig.ACKS_CONFIG, "all");

        // Retry transient failures
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 500);

        // Idempotent producer: exactly-once delivery, no duplicate messages on retry
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Batching for throughput
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 5);

        // Compression for network efficiency
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // JSON serializer: include type info for deserialization
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, AuditLogDTO> kafkaTemplate() {
        return new KafkaTemplate<>(auditProducerFactory());
    }
}
