package com.ecommerce.common.util;

import java.util.UUID;

public final class RequestIdGenerator {

    /** Private constructor - utility class */
    private RequestIdGenerator() {
        throw new UnsupportedOperationException("RequestIdGenerator is a utility class");
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    public static String generateShort() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }

    public static boolean isValid(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return false;
        }
        // Try UUID format first
        try {
            UUID.fromString(requestId);
            return true;
        } catch (IllegalArgumentException e) {
            // Not UUID format - accept if at least 8 chars
            return requestId.length() >= 8;
        }
    }

    public static String ensureValid(String requestId) {
        return isValid(requestId) ? requestId : generate();
    }
}
