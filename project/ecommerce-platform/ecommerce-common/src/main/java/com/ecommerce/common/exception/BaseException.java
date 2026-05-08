package com.ecommerce.common.exception;

import com.ecommerce.common.enums.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;
    private final String requestId;

    public BaseException(ErrorCode errorCode, HttpStatus httpStatus, String message, String requestId) {
        super(message);
        this.errorCode  = errorCode;
        this.httpStatus = httpStatus;
        this.requestId  = requestId;
    }

    public BaseException(ErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode  = errorCode;
        this.httpStatus = httpStatus;
        this.requestId  = null;
    }

    public BaseException(ErrorCode errorCode, HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.errorCode  = errorCode;
        this.httpStatus = httpStatus;
        this.requestId  = null;
    }
}
