package com.ecommerce.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public final class DateTimeUtil {

    /** Private constructor - utility class */
    private DateTimeUtil() {
        throw new UnsupportedOperationException("DateTimeUtil is a utility class");
    }

    // =========================================================================
    // DATE-TIME FORMATS
    // =========================================================================

    /** Standard API format: "2024-01-15T10:30:00" - used in all JSON responses */
    public static final DateTimeFormatter DEFAULT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** Human-readable format: "15-Jan-2024 10:30:00" - used in emails, reports */
    public static final DateTimeFormatter READABLE_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    /** Date-only format: "2024-01-15" - used in date range filters */
    public static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Compact format for file names: "20240115_103000" */
    public static final DateTimeFormatter FILE_NAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** Indian Standard Time zone */
    public static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    // =========================================================================
    // CURRENT TIME
    // =========================================================================

    /**
     * Returns the current system LocalDateTime.
     * Use this consistently instead of LocalDateTime.now() directly,
     * so it can be mocked in unit tests.
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Returns the current date as LocalDate.
     */
    public static LocalDate today() {
        return LocalDate.now();
    }

    /**
     * Returns the current time in IST timezone as ZonedDateTime.
     */
    public static ZonedDateTime nowInIST() {
        return ZonedDateTime.now(IST_ZONE);
    }

    // =========================================================================
    // FORMATTING
    // =========================================================================

    /**
     * Formats a LocalDateTime to the standard API format: "2024-01-15T10:30:00"
     *
     * @param dateTime the date-time to format
     * @return formatted string, or null if input is null
     */
    public static String formatDefault(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DEFAULT_FORMATTER);
    }

    /**
     * Formats a LocalDateTime to human-readable: "15-Jan-2024 10:30:00"
     *
     * @param dateTime the date-time to format
     * @return formatted string, or null if input is null
     */
    public static String formatReadable(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(READABLE_FORMATTER);
    }

    /**
     * Formats a LocalDate to standard: "2024-01-15"
     */
    public static String formatDate(LocalDate date) {
        if (date == null) return null;
        return date.format(DATE_FORMATTER);
    }

    /**
     * Formats a LocalDateTime for use in file names: "20240115_103000"
     */
    public static String formatForFileName(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(FILE_NAME_FORMATTER);
    }

    // =========================================================================
    // PARSING
    // =========================================================================

    public static LocalDateTime parseDefault(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) return null;
        return LocalDateTime.parse(dateTimeStr, DEFAULT_FORMATTER);
    }

    /**
     * Parses a date string in format: "2024-01-15"
     *
     * @param dateStr the string to parse
     * @return parsed LocalDate, or null if input is null/blank
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }

    // =========================================================================
    // CALCULATIONS
    // =========================================================================

    public static boolean isExpired(LocalDateTime expiryTime) {
        if (expiryTime == null) return true;
        return LocalDateTime.now().isAfter(expiryTime);
    }

    /**
     * Returns the number of minutes between two date-times.
     * Useful for calculating token age, order processing time, etc.
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * Returns the number of hours between two date-times.
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * Returns the number of days between two dates.
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Adds minutes to a LocalDateTime. Useful for calculating token expiry.
     *
     * USAGE:
     *   LocalDateTime expiresAt = DateTimeUtil.addMinutes(LocalDateTime.now(), 60);
     */
    public static LocalDateTime addMinutes(LocalDateTime dateTime, long minutes) {
        return dateTime.plusMinutes(minutes);
    }

    /**
     * Adds hours to a LocalDateTime.
     */
    public static LocalDateTime addHours(LocalDateTime dateTime, long hours) {
        return dateTime.plusHours(hours);
    }

    /**
     * Converts a LocalDateTime to IST ZonedDateTime.
     * Useful for displaying times to Indian users.
     */
    public static ZonedDateTime toIST(LocalDateTime localDateTime) {
        return localDateTime.atZone(IST_ZONE);
    }
}
