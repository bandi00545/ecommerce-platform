package com.ecommerce.orderservice.dto.response;

import com.ecommerce.common.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderResponse {
    private String id;
    private String userId;
    private String requestId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String notes;
    private String paymentId;
    private String transactionId;
    private String failureReason;
    private List<OrderItemResponse> items;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
