package com.ecommerce.productservice.config;

import com.ecommerce.common.context.RequestContext;
import com.ecommerce.common.dto.audit.AuditLogDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

@Configuration
@EnableWebSecurity
public class ProductServiceConfig {

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // =========================================================================
    // JPA AUDITING
    // =========================================================================

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(RequestContext.getUserId());
    }

    // =========================================================================
    // REDIS
    // =========================================================================

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "spring.cache", name = "type", havingValue = "redis")
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "spring.cache", name = "type", havingValue = "redis")
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                      ObjectMapper objectMapper) {

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("products",
                        defaultConfig.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("productPages",
                        defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .build();
    }

    // =========================================================================
    // KAFKA PRODUCER (AuditLogDTO)
    // =========================================================================

    @Bean
    public ProducerFactory<String, AuditLogDTO> auditProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,        bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,     StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,   JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG,                     "all");
        config.put(ProducerConfig.RETRIES_CONFIG,                  3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,       true);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS,           false);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, AuditLogDTO> kafkaTemplate() {
        return new KafkaTemplate<>(auditProducerFactory());
    }

    // =========================================================================
    // ASYNC EXECUTOR (for @Async audit publishing)
    // =========================================================================

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("product-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
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
                // Public: reading products doesn't require auth
                .requestMatchers(
                    "/api/v1/products",
                    "/api/v1/products/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/swagger-ui.html", "/swagger-ui/**",
                    "/v3/api-docs", "/v3/api-docs/**",
                    "/swagger-resources/**", "/webjars/**"
                ).permitAll()
                // Write operations require ADMIN (enforced at Gateway, but double-checked here)
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
