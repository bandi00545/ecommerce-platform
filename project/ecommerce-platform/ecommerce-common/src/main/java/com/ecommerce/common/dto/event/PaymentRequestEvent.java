package com.ecommerce.common.dto.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentRequestEvent {
    private String orderId;
    private String requestId;
    private String userId;
    private BigDecimal amount;
    private LocalDateTime requestedAt;
}
