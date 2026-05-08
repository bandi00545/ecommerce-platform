package com.ecommerce.common.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    /** Date format used consistently across all services */
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String DATE_FORMAT       = "yyyy-MM-dd";

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // ====================================================================
        // JAVA TIME MODULE - Required for LocalDateTime, LocalDate support
        // ====================================================================
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // LocalDateTime: serialize as "2024-01-15T10:30:00"
        javaTimeModule.addSerializer(
                LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))
        );
        javaTimeModule.addDeserializer(
                LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))
        );

        // LocalDate: serialize as "2024-01-15"
        javaTimeModule.addSerializer(
                LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT))
        );
        javaTimeModule.addDeserializer(
                LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT))
        );

        mapper.registerModule(javaTimeModule);

        // ====================================================================
        // SERIALIZATION FEATURES
        // ====================================================================

        // CRITICAL: Serialize dates as ISO strings, NOT as numeric timestamps [2024,1,15,10,30,0]
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Don't fail when serializing empty beans (POJOs with no properties)
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // Sort map/object keys alphabetically in output (easier to read in logs)
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

        // ====================================================================
        // DESERIALIZATION FEATURES
        // ====================================================================

        // CRITICAL: Don't fail when JSON has unknown fields not in the Java class.
        // This enables backward-compatible rolling deploys where different service
        // versions may have different field sets.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // Treat single-element arrays as single values
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        // Don't fail on null primitives (e.g. null for int field → 0)
        mapper.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

        // ====================================================================
        // INCLUSION - controls which fields appear in JSON output
        // ====================================================================

        // NON_NULL: Omit null fields from output
        // e.g. "errorCode": null won't appear in success responses
        // This is overridden per-class with @JsonInclude if needed
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // ====================================================================
        // NAMING STRATEGY
        // ====================================================================

        // camelCase (Spring default): firstName, orderId, createdAt
        // This is the default but explicit declaration prevents accidental overrides
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

        return mapper;
    }
}
