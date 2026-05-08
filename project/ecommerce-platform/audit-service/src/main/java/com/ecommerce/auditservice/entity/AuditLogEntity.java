package com.ecommerce.auditservice.entity;

import com.ecommerce.common.enums.AuditStatus;
import com.ecommerce.common.enums.ServiceName;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_request_id",  columnList = "request_id"),
        @Index(name = "idx_audit_user_id",     columnList = "user_id"),
        @Index(name = "idx_audit_service",     columnList = "service_name"),
        @Index(name = "idx_audit_action",      columnList = "action"),
        @Index(name = "idx_audit_status",      columnList = "status"),
        @Index(name = "idx_audit_timestamp",   columnList = "event_timestamp DESC"),
        @Index(name = "idx_audit_entity",      columnList = "entity_type, entity_id"),
        // Composite index for the most common combined query
        @Index(name = "idx_audit_service_status_ts",
               columnList = "service_name, status, event_timestamp")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AuditLogEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    /**
     * Request trace ID — links all log entries from the same HTTP request.
     * Indexed for fast lookup: "SELECT * FROM audit_logs WHERE request_id = ?"
     */
    @Column(name = "request_id", length = 36)
    private String requestId;

    /** ID of the user who triggered the action. Null for system actions. */
    @Column(name = "user_id", length = 36)
    private String userId;

    /** Which microservice generated this log entry */
    @Enumerated(EnumType.STRING)
    @Column(name = "service_name", nullable = false, length = 30)
    private ServiceName serviceName;

    /**
     * Action performed. Always use AppConstants.ACTION_* constants.
     * Examples: LOGIN, ORDER_CREATE, PAYMENT, STOCK_REDUCE, REGISTER
     */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    /** Outcome: SUCCESS, FAILURE, or PARTIAL */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuditStatus status;

    /** Human-readable description of what happened */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Error message if status=FAILURE. Null on SUCCESS. */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /** Client IP from X-Forwarded-For header (set by API Gateway) */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /** Browser/client User-Agent string */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /** Type of entity affected: "Order", "Payment", "User", "Product" */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /** ID of the affected entity (orderId, paymentId, userId, productId) */
    @Column(name = "entity_id", length = 36)
    private String entityId;

    /**
     * When the event actually occurred (from AuditLogDTO.timestamp).
     * NOT the time this record was inserted into the DB.
     * Named event_timestamp to avoid collision with DB reserved word.
     */
    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    /**
     * When this record was inserted into audit_db.
     * Used to detect processing lag (event_timestamp vs inserted_at).
     */
    @Column(name = "inserted_at", nullable = false, updatable = false)
    private LocalDateTime insertedAt;

    @PrePersist
    protected void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.insertedAt == null) {
            this.insertedAt = LocalDateTime.now();
        }
        if (this.eventTimestamp == null) {
            this.eventTimestamp = LocalDateTime.now();
        }
    }
}
