package com.ecommerce.orderservice.entity;

import com.ecommerce.common.entity.BaseEntity;
import com.ecommerce.common.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_user_id",    columnList = "user_id"),
        @Index(name = "idx_orders_status",     columnList = "status"),
        @Index(name = "idx_orders_request_id", columnList = "request_id", unique = true),
        @Index(name = "idx_orders_created_at", columnList = "created_at")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class OrderEntity extends BaseEntity {

    /** ID of the user who placed this order. Validated against User Service. */
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    /**
     * The requestId from the original API request.
     * UNIQUE constraint prevents duplicate order creation on network retries.
     * This is the IDEMPOTENCY KEY: same requestId = same order, not a duplicate.
     */
    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    private String requestId;

    /** Current state in the order lifecycle */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /**
     * Total order amount = sum of (item.price * item.quantity) for all items.
     * Calculated at order creation and stored (price snapshot).
     * NOT recalculated on fetch - prices may change after order placed.
     */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /** Shipping address captured at order time */
    @Column(name = "shipping_address", nullable = false, length = 500)
    private String shippingAddress;

    /** Optional customer notes */
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Payment ID returned by Payment Service on successful payment.
     * Null until payment step in Saga succeeds.
     */
    @Column(name = "payment_id", length = 36)
    private String paymentId;

    /**
     * Transaction ID returned by Transaction Service.
     * Null until transaction recording step succeeds.
     */
    @Column(name = "transaction_id", length = 36)
    private String transactionId;

    /**
     * Reason for failure - set when status becomes PAYMENT_FAILED or TRANSACTION_FAILED.
     * Displayed to customer on order failure.
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<OrderItemEntity> items = new ArrayList<>();

    // =========================================================================
    // DOMAIN METHODS
    // =========================================================================

    /** Adds an item to the order and sets bidirectional relationship */
    public void addItem(OrderItemEntity item) {
        items.add(item);
        item.setOrder(this);
    }

    /** Calculates and sets totalAmount from items. Call before saving. */
    public void calculateTotal() {
        this.totalAmount = items.stream()
                .map(item -> item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
