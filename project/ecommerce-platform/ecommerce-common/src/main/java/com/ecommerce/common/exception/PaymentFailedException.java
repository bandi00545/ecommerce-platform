package com.ecommerce.common.exception;

import com.ecommerce.common.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class PaymentFailedException extends BaseException {

    public PaymentFailedException(String message) {
        super(ErrorCode.PAYMENT_FAILED, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public PaymentFailedException(String message, String requestId) {
        super(ErrorCode.PAYMENT_FAILED, HttpStatus.UNPROCESSABLE_ENTITY, message, requestId);
    }

    public PaymentFailedException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public PaymentFailedException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, HttpStatus.UNPROCESSABLE_ENTITY, message, cause);
    }
}
