package com.ecommerce.common.exception;

import com.ecommerce.common.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, message);
    }

    public ForbiddenException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.FORBIDDEN, message);
    }

    public ForbiddenException(String message, String requestId) {
        super(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, message, requestId);
    }
}
