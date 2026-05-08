package com.ecommerce.common.dto.audit;

import com.ecommerce.common.enums.AuditStatus;
import com.ecommerce.common.enums.ServiceName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {

    /**
     * Links this log entry to the originating request.
     * All log entries from the same HTTP request share this ID.
     * Essential for end-to-end tracing across services.
     */
    private String requestId;

    /**
     * ID of the user who triggered this action.
     * Null for system-initiated actions (scheduled jobs, etc.)
     */
    private String userId;

    /**
     * Which microservice generated this audit entry.
     * e.g. ServiceName.ORDER_SERVICE
     */
    private ServiceName serviceName;

    /**
     * What action was performed.
     * Use AppConstants.ACTION_* values for consistency.
     * e.g. "ORDER_CREATE", "PAYMENT", "LOGIN"
     */
    private String action;

    /**
     * Outcome of the action.
     * SUCCESS, FAILURE, or PARTIAL.
     */
    private AuditStatus status;

    /**
     * Human-readable description of what happened.
     * e.g. "Order #88 created with 3 items worth ₹1,250"
     * e.g. "Payment failed for order #88: Insufficient funds"
     */
    private String description;

    /**
     * Error message if status = FAILURE.
     * Contains the exception message or error detail.
     * Null on SUCCESS.
     */
    private String errorMessage;

    /**
     * Client IP address extracted from X-Forwarded-For header.
     * Useful for security audits and fraud detection.
     */
    private String ipAddress;

    /**
     * Browser/client User-Agent string.
     * Useful for identifying client types and fraud patterns.
     */
    private String userAgent;

    /**
     * Entity type that was affected, e.g. "Order", "Payment", "User"
     * Optional - helps with filtering audit logs by entity type.
     */
    private String entityType;

    /**
     * ID of the entity that was affected, e.g. "88" for orderId=88.
     * Combined with entityType, enables efficient lookup:
     *   "Show all audit events for Order #88"
     */
    private String entityId;

    /**
     * Server-side timestamp when the action occurred.
     * Set automatically if not provided.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
