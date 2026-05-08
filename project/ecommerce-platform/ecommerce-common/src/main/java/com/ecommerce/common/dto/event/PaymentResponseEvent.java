package com.ecommerce.common.dto.event;

import com.ecommerce.common.enums.PaymentStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentResponseEvent {
    private String orderId;
    private String requestId;
    private String paymentId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String failureReason;
    private LocalDateTime processedAt;
}
