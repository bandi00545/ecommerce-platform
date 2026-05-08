package com.ecommerce.common.dto.event;

import com.ecommerce.common.enums.TransactionStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TransactionResponseEvent {
    private String orderId;
    private String requestId;
    private String transactionId;
    private TransactionStatus status;
    private BigDecimal amount;
    private String failureReason;
    private LocalDateTime processedAt;
}
