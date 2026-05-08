package com.ecommerce.common.enums;

public enum ErrorCode {

    // =====================================================================
    // 0xxx - GENERAL / CROSS-CUTTING
    // =====================================================================
    INTERNAL_SERVER_ERROR("ERR_0001", "Internal server error"),
    VALIDATION_FAILED("ERR_0002", "Validation failed"),
    RESOURCE_NOT_FOUND("ERR_0003", "Resource not found"),
    DUPLICATE_RESOURCE("ERR_0004", "Resource already exists"),
    UNAUTHORIZED("ERR_0005", "Unauthorized access"),
    FORBIDDEN("ERR_0006", "Access forbidden"),
    BAD_REQUEST("ERR_0007", "Bad request"),
    SERVICE_UNAVAILABLE("ERR_0008", "Service temporarily unavailable"),
    METHOD_NOT_ALLOWED("ERR_0009", "HTTP method not allowed"),
    MEDIA_TYPE_NOT_SUPPORTED("ERR_0010", "Media type not supported"),

    // =====================================================================
    // 1xxx - USER SERVICE
    // =====================================================================
    USER_NOT_FOUND("ERR_1001", "User not found"),
    USER_ALREADY_EXISTS("ERR_1002", "User with this email already exists"),
    INVALID_CREDENTIALS("ERR_1003", "Invalid email or password"),
    ACCOUNT_DISABLED("ERR_1004", "User account is disabled"),
    TOKEN_EXPIRED("ERR_1005", "JWT token has expired"),
    TOKEN_INVALID("ERR_1006", "JWT token is invalid or malformed"),
    TOKEN_MISSING("ERR_1007", "JWT token is missing from request"),
    USERNAME_TAKEN("ERR_1008", "Username is already taken"),
    INVALID_ROLE("ERR_1009", "Invalid user role specified"),
    PASSWORD_TOO_WEAK("ERR_1010", "Password does not meet complexity requirements"),

    // =====================================================================
    // 2xxx - PRODUCT SERVICE
    // =====================================================================
    PRODUCT_NOT_FOUND("ERR_2001", "Product not found"),
    PRODUCT_OUT_OF_STOCK("ERR_2002", "Product is out of stock"),
    INSUFFICIENT_STOCK("ERR_2003", "Insufficient stock quantity for this order"),
    PRODUCT_ALREADY_EXISTS("ERR_2004", "Product with this SKU already exists"),
    INVALID_PRICE("ERR_2005", "Product price must be greater than zero"),
    INVALID_STOCK_QUANTITY("ERR_2006", "Stock quantity must be zero or greater"),
    CATEGORY_NOT_FOUND("ERR_2007", "Product category not found"),
    INVALID_SKU("ERR_2008", "Product SKU format is invalid"),

    // =====================================================================
    // 3xxx - ORDER SERVICE
    // =====================================================================
    ORDER_NOT_FOUND("ERR_3001", "Order not found"),
    ORDER_ALREADY_COMPLETED("ERR_3002", "Order has already been completed"),
    ORDER_CANNOT_BE_CANCELLED("ERR_3003", "Order cannot be cancelled in its current state"),
    EMPTY_ORDER_ITEMS("ERR_3004", "Order must contain at least one item"),
    INVALID_ORDER_STATE("ERR_3005", "Invalid order state transition"),
    ORDER_ALREADY_CANCELLED("ERR_3006", "Order has already been cancelled"),
    ORDER_BELONGS_TO_ANOTHER_USER("ERR_3007", "Order does not belong to this user"),
    SAGA_EXECUTION_FAILED("ERR_3008", "Order saga execution failed"),
    SAGA_COMPENSATION_FAILED("ERR_3009", "Saga compensation partially failed"),

    // =====================================================================
    // 4xxx - PAYMENT SERVICE
    // =====================================================================
    PAYMENT_FAILED("ERR_4001", "Payment processing failed"),
    PAYMENT_NOT_FOUND("ERR_4002", "Payment record not found"),
    PAYMENT_ALREADY_PROCESSED("ERR_4003", "Payment has already been processed"),
    INVALID_PAYMENT_AMOUNT("ERR_4004", "Payment amount must be greater than zero"),
    PAYMENT_GATEWAY_ERROR("ERR_4005", "Payment gateway returned an error"),
    REFUND_FAILED("ERR_4006", "Payment refund failed"),
    PAYMENT_CANCELLED("ERR_4007", "Payment was cancelled"),

    // =====================================================================
    // 5xxx - TRANSACTION SERVICE
    // =====================================================================
    TRANSACTION_FAILED("ERR_5001", "Transaction recording failed"),
    TRANSACTION_NOT_FOUND("ERR_5002", "Transaction record not found"),
    TRANSACTION_ALREADY_COMPLETED("ERR_5003", "Transaction has already been completed"),
    TRANSACTION_REVERSAL_FAILED("ERR_5004", "Transaction reversal failed"),
    LEDGER_WRITE_FAILED("ERR_5005", "Failed to write to financial ledger"),

    // =====================================================================
    // 6xxx - INFRASTRUCTURE / RESILIENCE
    // =====================================================================
    CIRCUIT_BREAKER_OPEN("ERR_6001", "Service circuit breaker is open - too many failures"),
    FALLBACK_TRIGGERED("ERR_6002", "Fallback response triggered due to service degradation"),
    RETRY_EXHAUSTED("ERR_6003", "All retry attempts have been exhausted"),
    RATE_LIMIT_EXCEEDED("ERR_6004", "Too many requests - rate limit exceeded"),
    GATEWAY_TIMEOUT("ERR_6005", "Gateway timeout - upstream service did not respond"),
    CACHE_ERROR("ERR_6006", "Cache operation failed"),
    DATABASE_ERROR("ERR_6007", "Database operation failed");

    // =====================================================================
    // FIELDS
    // =====================================================================
    private final String code;
    private final String defaultMessage;

    // =====================================================================
    // CONSTRUCTOR
    // =====================================================================
    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    // =====================================================================
    // ACCESSORS
    // =====================================================================
    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    @Override
    public String toString() {
        return code + ": " + defaultMessage;
    }
}
