package com.ecommerce.orderservice.dto.event;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderItemEvent {
    private String productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private BigDecimal unitPrice;
}
