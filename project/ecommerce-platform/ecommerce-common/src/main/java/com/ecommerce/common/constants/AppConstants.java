package com.ecommerce.common.constants;

public final class AppConstants {

    // =====================================================================
    // PRIVATE CONSTRUCTOR - utility class, never instantiate
    // =====================================================================
    private AppConstants() {
        throw new UnsupportedOperationException("AppConstants is a utility class");
    }

    // =====================================================================
    // HTTP HEADERS
    // =====================================================================
    /** Propagated across all inter-service calls for full request tracing */
    public static final String HEADER_REQUEST_ID   = "X-Request-Id";

    /** Set by API Gateway after JWT validation; downstream services trust this */
    public static final String HEADER_USER_ID      = "X-User-Id";

    /** Set by API Gateway after JWT validation; used for RBAC in services */
    public static final String HEADER_USER_ROLE    = "X-User-Role";

    /** Standard HTTP Authorization header */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /** Bearer token prefix in Authorization header */
    public static final String BEARER_PREFIX        = "Bearer ";

    /** Client IP forwarded by proxies/gateway */
    public static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";

    /** User-Agent header */
    public static final String HEADER_USER_AGENT   = "User-Agent";

    // =====================================================================
    // JWT CLAIM KEYS
    // =====================================================================
    public static final String CLAIM_USER_ID    = "userId";
    public static final String CLAIM_USERNAME   = "username";
    public static final String CLAIM_ROLE       = "role";
    public static final String CLAIM_EMAIL      = "email";

    // =====================================================================
    // PAGINATION DEFAULTS
    // =====================================================================
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE   = "10";
    public static final String DEFAULT_SORT_BY     = "createdAt";
    public static final String DEFAULT_SORT_DIR    = "desc";
    public static final int    MAX_PAGE_SIZE       = 100;

    // =====================================================================
    // REDIS CACHE PREFIXES & TTL
    // =====================================================================
    /** e.g. "user:42" */
    public static final String CACHE_USER_PREFIX        = "user:";

    /** e.g. "product:101" */
    public static final String CACHE_PRODUCT_PREFIX     = "product:";

    /** e.g. "product:page:0:10" */
    public static final String CACHE_PRODUCT_PAGE_PREFIX = "product:page:";

    /** e.g. "order:88" */
    public static final String CACHE_ORDER_PREFIX       = "order:";

    /** Default TTL for cached entries: 30 minutes */
    public static final long   CACHE_TTL_MINUTES        = 30L;

    /** Short TTL for frequently changing data: 5 minutes */
    public static final long   CACHE_SHORT_TTL_MINUTES  = 5L;

    // =====================================================================
    // AUDIT ACTION NAMES
    // These exact strings are stored in audit_logs.action column
    // =====================================================================
    public static final String ACTION_CREATE       = "CREATE";
    public static final String ACTION_UPDATE       = "UPDATE";
    public static final String ACTION_DELETE       = "DELETE";
    public static final String ACTION_READ         = "READ";
    public static final String ACTION_LOGIN        = "LOGIN";
    public static final String ACTION_LOGOUT       = "LOGOUT";
    public static final String ACTION_REGISTER     = "REGISTER";
    public static final String ACTION_PAYMENT      = "PAYMENT";
    public static final String ACTION_REFUND       = "REFUND";
    public static final String ACTION_ORDER_CREATE = "ORDER_CREATE";
    public static final String ACTION_ORDER_CANCEL = "ORDER_CANCEL";
    public static final String ACTION_TRANSACTION  = "TRANSACTION";
    public static final String ACTION_STOCK_REDUCE = "STOCK_REDUCE";
    public static final String ACTION_STOCK_RESTORE = "STOCK_RESTORE";

    // =====================================================================
    // STANDARD RESPONSE MESSAGES
    // =====================================================================
    public static final String MSG_SUCCESS          = "Operation completed successfully";
    public static final String MSG_CREATED          = "Resource created successfully";
    public static final String MSG_UPDATED          = "Resource updated successfully";
    public static final String MSG_DELETED          = "Resource deleted successfully";
    public static final String MSG_FETCHED          = "Resource fetched successfully";
    public static final String MSG_NOT_FOUND        = "Resource not found";
    public static final String MSG_UNAUTHORIZED     = "Unauthorized access";
    public static final String MSG_FORBIDDEN        = "Access forbidden";
    public static final String MSG_VALIDATION_FAILED = "Validation failed";
    public static final String MSG_INTERNAL_ERROR   = "Internal server error occurred";
    public static final String MSG_SERVICE_DOWN     = "Service temporarily unavailable";

    // =====================================================================
    // SAGA STEP NAMES (Order Service orchestration)
    // Stored in saga_log table for distributed transaction tracking
    // =====================================================================
    public static final String SAGA_STEP_ORDER_CREATED    = "ORDER_CREATED";
    public static final String SAGA_STEP_PAYMENT_INIT     = "PAYMENT_INITIATED";
    public static final String SAGA_STEP_PAYMENT_SUCCESS  = "PAYMENT_SUCCESS";
    public static final String SAGA_STEP_PAYMENT_FAILED   = "PAYMENT_FAILED";
    public static final String SAGA_STEP_TXN_RECORDED     = "TRANSACTION_RECORDED";
    public static final String SAGA_STEP_TXN_FAILED       = "TRANSACTION_FAILED";
    public static final String SAGA_STEP_ORDER_COMPLETE   = "ORDER_COMPLETE";
    public static final String SAGA_STEP_COMPENSATE_START = "COMPENSATION_STARTED";
    public static final String SAGA_STEP_COMPENSATE_DONE  = "COMPENSATION_COMPLETE";

    // =====================================================================
    // SERVICE URLS (used in service-to-service RestTemplate/Feign calls)
    // Actual values come from Eureka; these are logical service names
    // =====================================================================
    public static final String SERVICE_USER_NAME        = "user-service";
    public static final String SERVICE_PRODUCT_NAME     = "product-service";
    public static final String SERVICE_ORDER_NAME       = "order-service";
    public static final String SERVICE_PAYMENT_NAME     = "payment-service";
    public static final String SERVICE_TRANSACTION_NAME = "transaction-service";
    public static final String SERVICE_AUDIT_NAME       = "audit-service";

    // =====================================================================
    // RESILIENCE4J CIRCUIT BREAKER NAMES (must match application.yml)
    // =====================================================================
    public static final String CB_PAYMENT_SERVICE     = "paymentServiceCB";
    public static final String CB_TRANSACTION_SERVICE = "transactionServiceCB";
    public static final String CB_USER_SERVICE        = "userServiceCB";
    public static final String CB_PRODUCT_SERVICE     = "productServiceCB";
    public static final String CB_AUDIT_SERVICE       = "auditServiceCB";

    // =====================================================================
    // DATE-TIME FORMATS
    // =====================================================================
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DATE_FORMAT      = "yyyy-MM-dd";
    public static final String TIME_ZONE_IST    = "Asia/Kolkata";

    // =====================================================================
    // SECURITY
    // =====================================================================
    /** Paths that bypass JWT validation at API Gateway */
    public static final String[] PUBLIC_ENDPOINTS = {
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/actuator/health",
        "/actuator/info"
    };
}
