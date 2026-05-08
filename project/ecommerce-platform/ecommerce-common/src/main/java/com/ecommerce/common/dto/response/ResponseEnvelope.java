package com.ecommerce.common.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseEnvelope<T> {

    /**
     * Always "SUCCESS" or "FAILURE".
     * Never null. Frontend uses this as primary result indicator.
     */
    private String status;

    /**
     * Structured error code from ErrorCode enum.
     * Present ONLY on FAILURE responses. Null on SUCCESS.
     * e.g. "ERR_3001", "ERR_4001"
     */
    private String errorCode;

    /**
     * Human-readable message describing the outcome.
     * Success: "Order created successfully"
     * Failure: "Order not found with id: 99"
     */
    private String message;

    /**
     * The actual response payload. Generic for type safety.
     * Present ONLY on SUCCESS responses. Null (omitted) on FAILURE.
     */
    private T data;

    /**
     * Echoed requestId from the incoming request.
     * Allows clients to correlate requests with responses.
     * Also used by support teams: "Give me requestId from your browser"
     */
    private String requestId;

    /**
     * Server-side timestamp when response was generated.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // =========================================================================
    // STATIC FACTORY METHODS - use these instead of the builder in controllers
    // =========================================================================

    /**
     * Creates a SUCCESS response with data, custom message, and requestId.
     *
     * USAGE in controller:
     *   return ResponseEntity.ok(ResponseEnvelope.success(orderResponse, "Order created", requestId));
     */
    public static <T> ResponseEnvelope<T> success(T data, String message, String requestId) {
        return ResponseEnvelope.<T>builder()
                .status("SUCCESS")
                .message(message)
                .data(data)
                .requestId(requestId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a SUCCESS response with data and requestId using default message.
     */
    public static <T> ResponseEnvelope<T> success(T data, String requestId) {
        return success(data, "Operation completed successfully", requestId);
    }

    /**
     * Creates a SUCCESS response with only a message (no data body).
     * Use for: DELETE operations, state transitions, actions with no return data.
     */
    public static <T> ResponseEnvelope<T> successMessage(String message, String requestId) {
        return ResponseEnvelope.<T>builder()
                .status("SUCCESS")
                .message(message)
                .requestId(requestId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a FAILURE response with error code, message, and requestId.
     *
     * USAGE in GlobalExceptionHandler:
     *   return ResponseEnvelope.failure(ErrorCode.ORDER_NOT_FOUND.getCode(), ex.getMessage(), requestId);
     */
    public static <T> ResponseEnvelope<T> failure(String errorCode, String message, String requestId) {
        return ResponseEnvelope.<T>builder()
                .status("FAILURE")
                .errorCode(errorCode)
                .message(message)
                .requestId(requestId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Convenience check - true if status is SUCCESS.
     * Useful in service-to-service calls:
     *   if (response.isSuccess()) { ... }
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(this.status);
    }

    /**
     * Convenience check - true if status is FAILURE.
     */
    public boolean isFailure() {
        return "FAILURE".equals(this.status);
    }
}
