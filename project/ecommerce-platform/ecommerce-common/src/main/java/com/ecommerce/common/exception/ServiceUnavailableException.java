package com.ecommerce.common.exception;

import com.ecommerce.common.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends BaseException {

    /**
     * Creates exception with a descriptive message including the service name.
     *
     * @param serviceName e.g. "payment-service", "transaction-service"
     */
    public ServiceUnavailableException(String serviceName) {
        super(
            ErrorCode.SERVICE_UNAVAILABLE,
            HttpStatus.SERVICE_UNAVAILABLE,
            String.format("Service '%s' is temporarily unavailable. Please try again later.", serviceName)
        );
    }

    /**
     * Constructor with specific error code.
     * Use ErrorCode.CIRCUIT_BREAKER_OPEN when the CB has tripped.
     * Use ErrorCode.FALLBACK_TRIGGERED when returning a degraded response.
     */
    public ServiceUnavailableException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    /**
     * Constructor wrapping the original cause.
     * Preserves original exception for logging.
     */
    public ServiceUnavailableException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
