package com.ecommerce.orderservice.entity;

import com.ecommerce.common.enums.AuditStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "saga_logs",
    indexes = {
        @Index(name = "idx_saga_order_id", columnList = "order_id"),
        @Index(name = "idx_saga_step",     columnList = "step_name")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class SagaLogEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuditStatus status;

    @Column(name = "step_data", columnDefinition = "TEXT")
    private String stepData;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}
