package com.ecommerce.orderservice.entity;

import com.ecommerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(
    name = "order_items",
    indexes = {
        @Index(name = "idx_order_items_order_id",   columnList = "order_id"),
        @Index(name = "idx_order_items_product_id", columnList = "product_id")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class OrderItemEntity extends BaseEntity {

    /** Parent order - owning side of the relationship */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    /** Product ID reference (no FK - cross-service, product in different DB) */
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    /** Product name snapshot (in case product name changes later) */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    /** Product SKU snapshot */
    @Column(name = "product_sku", nullable = false, length = 50)
    private String productSku;

    /** Number of units ordered */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Price per unit AT ORDER PLACEMENT TIME.
     * This is a snapshot - never update this after order created.
     */
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** Convenience method: total for this line item */
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
