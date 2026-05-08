package com.ecommerce.common.exception;

import com.ecommerce.common.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
            ErrorCode.RESOURCE_NOT_FOUND,
            HttpStatus.NOT_FOUND,
            String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue)
        );
    }

    /**
     * Constructor with requestId - preferred when requestId is available.
     */
    public ResourceNotFoundException(String resourceName, String fieldName,
                                      Object fieldValue, String requestId) {
        super(
            ErrorCode.RESOURCE_NOT_FOUND,
            HttpStatus.NOT_FOUND,
            String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue),
            requestId
        );
    }

    /**
     * Constructor with specific ErrorCode - use for domain-specific error codes.
     * e.g. ErrorCode.USER_NOT_FOUND, ErrorCode.ORDER_NOT_FOUND
     */
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.NOT_FOUND, message);
    }

    /**
     * Constructor with specific ErrorCode and requestId.
     */
    public ResourceNotFoundException(ErrorCode errorCode, String message, String requestId) {
        super(errorCode, HttpStatus.NOT_FOUND, message, requestId);
    }
}
