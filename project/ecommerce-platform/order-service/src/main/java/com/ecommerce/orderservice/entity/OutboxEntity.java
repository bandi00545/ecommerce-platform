package com.ecommerce.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "outbox_events",
    indexes = {
        @Index(name = "idx_outbox_processed",   columnList = "processed"),
        @Index(name = "idx_outbox_created_at",  columnList = "created_at"),
        @Index(name = "idx_outbox_aggregate_id",columnList = "aggregate_id")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class OutboxEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    /** Aggregate type: "Order", "Payment", etc. */
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    /** The order ID (or other aggregate ID) this event relates to */
    @Column(name = "aggregate_id", nullable = false, length = 36)
    private String aggregateId;

    /** Kafka topic this event should be published to */
    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    /** JSON serialized event payload */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    /** Event type name e.g. "ORDER_CREATED", "PAYMENT_REQUESTED" */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /** Kafka message key (usually requestId for ordering) */
    @Column(name = "message_key", length = 36)
    private String messageKey;

    /** false = not yet published to Kafka; true = successfully published */
    @Column(name = "processed", nullable = false)
    @Builder.Default
    private boolean processed = false;

    /** When this event was created (same transaction as the domain event) */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** When this event was successfully published to Kafka */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /** Number of publish attempts (for monitoring stuck events) */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    /** Last error if publish failed */
    @Column(name = "last_error", length = 500)
    private String lastError;

    @PrePersist
    protected void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}
