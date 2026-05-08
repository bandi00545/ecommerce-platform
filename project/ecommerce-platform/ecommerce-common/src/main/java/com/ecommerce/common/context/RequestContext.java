package com.ecommerce.common.context;

public final class RequestContext {

    // ThreadLocal for each piece of context data
    private static final ThreadLocal<String> REQUEST_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ID_HOLDER    = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ROLE_HOLDER  = new ThreadLocal<>();

    /** Private constructor - utility class, never instantiate */
    private RequestContext() {
        throw new UnsupportedOperationException("RequestContext is a utility class");
    }

    // =========================================================================
    // REQUEST ID
    // =========================================================================

    /**
     * Sets the requestId for the current thread.
     * Called by RequestContextFilter at the start of every request.
     */
    public static void setRequestId(String requestId) {
        REQUEST_ID_HOLDER.set(requestId);
    }

    /**
     * Returns the requestId for the current thread.
     * Returns null if not set (e.g. in async threads).
     */
    public static String getRequestId() {
        return REQUEST_ID_HOLDER.get();
    }

    // =========================================================================
    // USER ID
    // =========================================================================

    /**
     * Sets the userId extracted from the JWT (via X-User-Id header set by API Gateway).
     */
    public static void setUserId(String userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * Returns the authenticated user's ID for the current request.
     * Returns null for unauthenticated requests (login, register endpoints).
     */
    public static String getUserId() {
        return USER_ID_HOLDER.get();
    }

    // =========================================================================
    // USER ROLE
    // =========================================================================

    /**
     * Sets the user role extracted from the JWT (via X-User-Role header set by API Gateway).
     */
    public static void setUserRole(String userRole) {
        USER_ROLE_HOLDER.set(userRole);
    }

    /**
     * Returns the authenticated user's role for the current request.
     * Returns null for unauthenticated requests.
     * Compare with UserRole.ADMIN.name() for role checks.
     */
    public static String getUserRole() {
        return USER_ROLE_HOLDER.get();
    }

    // =========================================================================
    // CLEAR - MUST BE CALLED AT END OF EVERY REQUEST
    // =========================================================================

    public static void clear() {
        REQUEST_ID_HOLDER.remove();
        USER_ID_HOLDER.remove();
        USER_ROLE_HOLDER.remove();
    }

    // =========================================================================
    // CONVENIENCE METHODS
    // =========================================================================

    /**
     * Returns true if there is an authenticated user in the current context.
     */
    public static boolean isAuthenticated() {
        return getUserId() != null && !getUserId().isBlank();
    }

    /**
     * Returns true if the current user has ADMIN role.
     */
    public static boolean isAdmin() {
        return "ADMIN".equals(getUserRole());
    }

    /**
     * Returns requestId or "UNKNOWN" - never returns null.
     * Safe to use in log statements directly.
     */
    public static String getRequestIdSafe() {
        String id = REQUEST_ID_HOLDER.get();
        return (id != null && !id.isBlank()) ? id : "UNKNOWN";
    }
}
