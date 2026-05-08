package com.ecommerce.common.exception;

import com.ecommerce.common.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class TransactionFailedException extends BaseException {

    public TransactionFailedException(String message) {
        super(ErrorCode.TRANSACTION_FAILED, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public TransactionFailedException(String message, String requestId) {
        super(ErrorCode.TRANSACTION_FAILED, HttpStatus.UNPROCESSABLE_ENTITY, message, requestId);
    }

    public TransactionFailedException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public TransactionFailedException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, HttpStatus.UNPROCESSABLE_ENTITY, message, cause);
    }
}
