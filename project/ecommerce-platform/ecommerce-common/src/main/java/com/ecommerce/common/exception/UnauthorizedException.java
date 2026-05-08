package com.ecommerce.common.exception;

import com.ecommerce.common.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, message);
    }

    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.UNAUTHORIZED, message);
    }

    public UnauthorizedException(ErrorCode errorCode, String message, String requestId) {
        super(errorCode, HttpStatus.UNAUTHORIZED, message, requestId);
    }
}
