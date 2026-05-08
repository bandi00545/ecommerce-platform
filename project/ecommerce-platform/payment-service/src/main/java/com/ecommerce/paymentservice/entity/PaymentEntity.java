package com.ecommerce.paymentservice.entity;

import com.ecommerce.common.entity.BaseEntity;
import com.ecommerce.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_order_id",   columnList = "order_id",   unique = true),
    @Index(name = "idx_payments_request_id", columnList = "request_id", unique = true),
    @Index(name = "idx_payments_status",     columnList = "status"),
    @Index(name = "idx_payments_user_id",    columnList = "user_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class PaymentEntity extends BaseEntity {

    @Column(name = "order_id",   nullable = false, unique = true, length = 36) private String orderId;
    @Column(name = "request_id", nullable = false, unique = true, length = 36) private String requestId;
    @Column(name = "user_id",    nullable = false, length = 36) private String userId;
    @Column(name = "amount",     nullable = false, precision = 12, scale = 2)  private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "failure_reason", length = 500) private String failureReason;
    @Column(name = "gateway_reference", length = 100) private String gatewayReference;
    @Column(name = "refunded", nullable = false) @Builder.Default private boolean refunded = false;
}
