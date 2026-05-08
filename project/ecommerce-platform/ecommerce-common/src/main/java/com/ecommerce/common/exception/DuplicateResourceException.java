package com.ecommerce.common.exception;

import com.ecommerce.common.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BaseException {

    /**
     * Standard constructor - generates descriptive message from parts.
     */
    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(
            ErrorCode.DUPLICATE_RESOURCE,
            HttpStatus.CONFLICT,
            String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue)
        );
    }

    /**
     * Custom message constructor.
     */
    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, HttpStatus.CONFLICT, message);
    }

    /**
     * Constructor with specific error code.
     * e.g. ErrorCode.USER_ALREADY_EXISTS, ErrorCode.PRODUCT_ALREADY_EXISTS
     */
    public DuplicateResourceException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.CONFLICT, message);
    }
}
