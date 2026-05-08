package com.ecommerce.common.util;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.regex.Pattern;

public final class ValidationUtil {

    /** Private constructor - utility class */
    private ValidationUtil() {
        throw new UnsupportedOperationException("ValidationUtil is a utility class");
    }

    // =========================================================================
    // REGEX PATTERNS
    // =========================================================================

    /**
     * Standard email validation pattern.
     * Accepts: john.doe@example.com, user+tag@domain.co.in
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Indian mobile phone number pattern.
     * Starts with 6-9, followed by 9 digits. Total 10 digits.
     * Accepts: 9876543210, 6543219876
     */
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[6-9]\\d{9}$");

    /**
     * Password complexity pattern.
     * Requires: min 8 chars, at least one uppercase, one lowercase,
     * one digit, one special char (@#$%^&+=!), no whitespace.
     */
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$");

    /**
     * Username pattern: 3-20 characters, alphanumeric and underscores only.
     */
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    // =========================================================================
    // STRING VALIDATION
    // =========================================================================

    /**
     * Returns true if the string is null or contains only whitespace.
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isBlank();
    }

    /**
     * Returns true if the string is NOT null and NOT blank.
     */
    public static boolean hasValue(String str) {
        return str != null && !str.isBlank();
    }

    /**
     * Returns true if the string length is within [min, max] inclusive.
     */
    public static boolean isLengthInRange(String str, int min, int max) {
        if (str == null) return false;
        int len = str.trim().length();
        return len >= min && len <= max;
    }

    // =========================================================================
    // COLLECTION VALIDATION
    // =========================================================================

    /**
     * Returns true if the collection is null or empty.
     */
    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Returns true if the collection is not null and not empty.
     */
    public static boolean hasElements(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    // =========================================================================
    // NUMERIC VALIDATION
    // =========================================================================

    /**
     * Returns true if the BigDecimal value is strictly greater than zero.
     * Use for: prices, payment amounts, quantities in decimal form.
     */
    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns true if the BigDecimal value is zero or greater.
     * Use for: stock quantities (can be zero if out of stock, not negative).
     */
    public static boolean isPositiveOrZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * Returns true if the Integer value is strictly greater than zero.
     * Use for: order quantities, item counts.
     */
    public static boolean isPositive(Integer value) {
        return value != null && value > 0;
    }

    /**
     * Returns true if the Long value is strictly greater than zero.
     * Use for: ID validation.
     */
    public static boolean isPositive(Long value) {
        return value != null && value > 0;
    }

    /**
     * Returns true if value1 is greater than or equal to value2.
     * Use for: checking available stock >= requested quantity.
     */
    public static boolean isGreaterOrEqual(BigDecimal value1, BigDecimal value2) {
        if (value1 == null || value2 == null) return false;
        return value1.compareTo(value2) >= 0;
    }

    // =========================================================================
    // FORMAT VALIDATION
    // =========================================================================

    /**
     * Validates email format.
     *
     * @param email the email address to validate
     * @return true if the email matches the standard email pattern
     */
    public static boolean isValidEmail(String email) {
        if (isNullOrEmpty(email)) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (isNullOrEmpty(phone)) return false;
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public static boolean isStrongPassword(String password) {
        if (isNullOrEmpty(password)) return false;
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean isValidUsername(String username) {
        if (isNullOrEmpty(username)) return false;
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }
}
