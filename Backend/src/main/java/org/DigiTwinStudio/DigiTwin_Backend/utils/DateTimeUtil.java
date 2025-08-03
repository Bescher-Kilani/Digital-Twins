package org.DigiTwinStudio.DigiTwin_Backend.utils;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Contains Help-Methods to assist in working with dates and timestamps.
 * As Spring Component can be used anywhere.
 */
@Component
public class DateTimeUtil {

    // ISO-8601-conform Formatter, without offset (e.g., 2025-07-13T15:30:00)
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Formats LocalDateTime-Object to ISO-8601-conform String.
     *
     * @param dateTime LocalDateTime to be formated
     * @return ISO-8601-conform String (e.g., "2025-07-13T15:30:00")
     * @throws IllegalArgumentException when dateTime object is null
     */
    public static String formatIsoLocalDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("dateTime can not be null");
        }
        return dateTime.format(ISO_FORMATTER);
    }

    /**
     * Parse ISO-8601-conform String to LocalDateTime-Object.
     *
     * @param dateTimeString ISO-8601-conform String
     * @return parsed LocalDateTime-Object
     * @throws DateTimeParseException when String does not match Format
     */
    public static LocalDateTime parseIsoLocalDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            throw new IllegalArgumentException("dateTimeString can not be null or empty");
        }
        return LocalDateTime.parse(dateTimeString, ISO_FORMATTER);  // includes DateTimeParseException
    }

    /**
     * Returns current UTC-time as LocalDateTime-Object
     *
     * @return current date and time in UTC
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(Clock.systemUTC());
    }
}
