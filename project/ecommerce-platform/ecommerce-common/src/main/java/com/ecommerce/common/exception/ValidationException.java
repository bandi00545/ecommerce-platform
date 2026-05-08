package com.ecommerce.common.exception;

import com.ecommerce.common.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class ValidationException extends BaseException {

    /**
     * Simple message constructor.
     */
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Constructor with requestId for tracing.
     */
    public ValidationException(String message, String requestId) {
        super(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message, requestId);
    }

    /**
     * Constructor with specific domain error code.
     * Use when a more specific error code than VALIDATION_FAILED is appropriate.
     * e.g. ErrorCode.ORDER_CANNOT_BE_CANCELLED
     */
    public ValidationException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Constructor with specific error code and requestId.
     */
    public ValidationException(ErrorCode errorCode, String message, String requestId) {
        super(errorCode, HttpStatus.BAD_REQUEST, message, requestId);
    }
}
