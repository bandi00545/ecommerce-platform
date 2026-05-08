package com.ecommerce.auditservice.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class AuditServiceConfig {

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:audit-service-group}")
    private String groupId;

    // =========================================================================
    // KAFKA BATCH CONSUMER FACTORY
    // =========================================================================

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,        bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG,                 groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,        "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // Manual commit: offset committed only after successful DB write
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,       false);
        // Batch size: fetch up to 50 records per poll
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,         50);
        // Wait up to 500ms or until 1KB of data before returning
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG,          1024);
        config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG,        500);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    batchKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true);       // ENABLE BATCH MODE
        factory.setConcurrency(4);            // 4 parallel consumer threads

        // Commit offset after entire batch is processed (not per message)
        factory.getContainerProperties().setAckMode(
                ContainerProperties.AckMode.BATCH
        );

        return factory;
    }

    // =========================================================================
    // SECURITY
    // =========================================================================

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/api/v1/audit/health",
                                 "/swagger-ui.html", "/swagger-ui/**",
                                 "/v3/api-docs", "/v3/api-docs/**",
                                 "/swagger-resources/**", "/webjars/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
