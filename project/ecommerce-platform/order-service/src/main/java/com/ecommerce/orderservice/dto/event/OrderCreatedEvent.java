package com.ecommerce.orderservice.dto.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderCreatedEvent {
    private String orderId;
    private String requestId;
    private String userId;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private List<OrderItemEvent> items;
    private LocalDateTime createdAt;
}
