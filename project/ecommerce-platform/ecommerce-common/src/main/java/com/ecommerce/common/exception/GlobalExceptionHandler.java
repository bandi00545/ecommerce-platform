package com.ecommerce.common.exception;

import com.ecommerce.common.constants.AppConstants;
import com.ecommerce.common.dto.response.ResponseEnvelope;
import com.ecommerce.common.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================================
    // 1. OUR CUSTOM EXCEPTIONS (BaseException hierarchy)
    // =========================================================================

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ResponseEnvelope<Void>> handleBaseException(
            BaseException ex,
            HttpServletRequest request) {

        String requestId = resolveRequestId(ex.getRequestId(), request);

        log.error("BaseException | requestId={} | errorCode={} | status={} | message={}",
                requestId, ex.getErrorCode().getCode(), ex.getHttpStatus(), ex.getMessage());

        ResponseEnvelope<Void> response = ResponseEnvelope.failure(
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                requestId
        );

        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(response);
    }

    // =========================================================================
    // 2. SPRING VALIDATION EXCEPTION (@Valid on request DTOs)
    // =========================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseEnvelope<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String requestId = resolveRequestId(null, request);
        Map<String, String> fieldErrors = new HashMap<>();

        // Extract field-level errors
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        // Extract class-level errors (cross-field validation)
        for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
            fieldErrors.put(globalError.getObjectName(), globalError.getDefaultMessage());
        }

        log.warn("Validation failed | requestId={} | errors={}", requestId, fieldErrors);

        ResponseEnvelope<Map<String, String>> response = ResponseEnvelope.<Map<String, String>>builder()
                .status("FAILURE")
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .message("Validation failed. Please check the request fields.")
                .data(fieldErrors)
                .requestId(requestId)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // =========================================================================
    // 3. STANDARD SPRING EXCEPTIONS
    // =========================================================================

    /**
     * Handles wrong HTTP method (e.g. POST to a GET-only endpoint).
     * Maps to HTTP 405 METHOD_NOT_ALLOWED.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseEnvelope<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        String requestId = resolveRequestId(null, request);
        String message = String.format(
                "HTTP method '%s' is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(), ex.getSupportedHttpMethods()
        );

        log.warn("Method not allowed | requestId={} | message={}", requestId, message);

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ResponseEnvelope.failure(
                        ErrorCode.METHOD_NOT_ALLOWED.getCode(), message, requestId
                ));
    }

    /**
     * Handles unsupported media type (e.g. sending XML to a JSON endpoint).
     * Maps to HTTP 415 UNSUPPORTED_MEDIA_TYPE.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ResponseEnvelope<Void>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {

        String requestId = resolveRequestId(null, request);
        String message = String.format(
                "Media type '%s' is not supported. Please use 'application/json'.",
                ex.getContentType()
        );

        log.warn("Media type not supported | requestId={} | contentType={}", requestId, ex.getContentType());

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ResponseEnvelope.failure(
                        ErrorCode.MEDIA_TYPE_NOT_SUPPORTED.getCode(), message, requestId
                ));
    }

    /**
     * Handles malformed JSON in the request body.
     * e.g. unclosed braces, wrong data type for a field.
     * Maps to HTTP 400 BAD_REQUEST.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseEnvelope<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        String requestId = resolveRequestId(null, request);

        log.warn("Malformed JSON in request | requestId={} | error={}", requestId, ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseEnvelope.failure(
                        ErrorCode.BAD_REQUEST.getCode(),
                        "Request body is malformed or contains invalid JSON",
                        requestId
                ));
    }

    /**
     * Handles missing required request parameters (@RequestParam).
     * Maps to HTTP 400 BAD_REQUEST.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ResponseEnvelope<Void>> handleMissingRequestParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        String requestId = resolveRequestId(null, request);
        String message = String.format("Required request parameter '%s' is missing", ex.getParameterName());

        log.warn("Missing request parameter | requestId={} | param={}", requestId, ex.getParameterName());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseEnvelope.failure(
                        ErrorCode.VALIDATION_FAILED.getCode(), message, requestId
                ));
    }

    /**
     * Handles missing required request headers (e.g. X-Request-Id if made mandatory).
     * Maps to HTTP 400 BAD_REQUEST.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ResponseEnvelope<Void>> handleMissingRequestHeader(
            MissingRequestHeaderException ex,
            HttpServletRequest request) {

        String requestId = resolveRequestId(null, request);
        String message = String.format("Required request header '%s' is missing", ex.getHeaderName());

        log.warn("Missing request header | requestId={} | header={}", requestId, ex.getHeaderName());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseEnvelope.failure(
                        ErrorCode.VALIDATION_FAILED.getCode(), message, requestId
                ));
    }

    /**
     * Handles type mismatch in path variables or request params.
     * e.g. /orders/abc when id expects Long.
     * Maps to HTTP 400 BAD_REQUEST.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseEnvelope<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String requestId = resolveRequestId(null, request);
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format(
                "Parameter '%s' must be of type '%s'. Received: '%s'",
                ex.getName(), requiredType, ex.getValue()
        );

        log.warn("Type mismatch | requestId={} | message={}", requestId, message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseEnvelope.failure(
                        ErrorCode.BAD_REQUEST.getCode(), message, requestId
                ));
    }

    /**
     * Handles IllegalArgumentException thrown manually in business code.
     * Maps to HTTP 400 BAD_REQUEST.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseEnvelope<Void>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        String requestId = resolveRequestId(null, request);

        log.warn("IllegalArgumentException | requestId={} | message={}", requestId, ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseEnvelope.failure(
                        ErrorCode.BAD_REQUEST.getCode(), ex.getMessage(), requestId
                ));
    }

    // =========================================================================
    // 4. CATCH-ALL FALLBACK (unexpected exceptions)
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseEnvelope<Void>> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        String requestId = resolveRequestId(null, request);

        // Log full stack trace at ERROR level - visible in log aggregators
        log.error("Unexpected error | requestId={} | type={} | message={}",
                requestId, ex.getClass().getSimpleName(), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseEnvelope.failure(
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        AppConstants.MSG_INTERNAL_ERROR,
                        requestId
                ));
    }

    // =========================================================================
    // PRIVATE HELPER
    // =========================================================================

    /**
     * Resolves requestId with priority:
     * 1. requestId from the exception itself (if set when throwing)
     * 2. X-Request-Id header from the HTTP request
     * 3. "UNKNOWN" as final fallback
     */
    private String resolveRequestId(String exceptionRequestId, HttpServletRequest request) {
        if (exceptionRequestId != null && !exceptionRequestId.isBlank()) {
            return exceptionRequestId;
        }
        String headerRequestId = request.getHeader(AppConstants.HEADER_REQUEST_ID);
        if (headerRequestId != null && !headerRequestId.isBlank()) {
            return headerRequestId;
        }
        return "UNKNOWN";
    }
}
