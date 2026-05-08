package com.ecommerce.transactionservice.entity;

import com.ecommerce.common.entity.BaseEntity;
import com.ecommerce.common.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_txn_order_id",   columnList = "order_id",   unique = true),
    @Index(name = "idx_txn_payment_id", columnList = "payment_id", unique = true),
    @Index(name = "idx_txn_status",     columnList = "status"),
    @Index(name = "idx_txn_user_id",    columnList = "user_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class TransactionEntity extends BaseEntity {

    @Column(name = "order_id",   nullable = false, unique = true, length = 36) private String orderId;
    @Column(name = "payment_id", nullable = false, unique = true, length = 36) private String paymentId;
    @Column(name = "request_id", nullable = false, length = 36) private String requestId;
    @Column(name = "user_id",    nullable = false, length = 36) private String userId;
    @Column(name = "amount",     nullable = false, precision = 12, scale = 2) private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.INITIATED;

    @Column(name = "failure_reason",    length = 500) private String failureReason;
    @Column(name = "ledger_reference",  length = 100) private String ledgerReference;
    @Column(name = "reversed",          nullable = false) @Builder.Default private boolean reversed = false;
}
