package org.DigiTwinStudio.DigiTwin_Backend.utils;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Bietet Hilfsmethoden zur Verarbeitung und Formatierung von Datums- und Zeitwerten.
 * Als Spring-Komponente registriert, kann sie überall injiziert werden.
 */
@Component
public class DateTimeUtil {

    // ISO-8601-konformer Formatter, ohne Offset (z.B. 2025-07-13T15:30:00)
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Formatiert ein LocalDateTime-Objekt in einen ISO-8601-konformen String.
     *
     * @param dateTime das zu formatierende LocalDateTime
     * @return ISO-8601-konformer String (z.B. "2025-07-13T15:30:00")
     */
    public static String formatIsoLocalDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("dateTime darf nicht null sein");
        }
        return dateTime.format(ISO_FORMATTER);
    }

    /**
     * Parst einen ISO-8601-konformen String zurück zu einem LocalDateTime-Objekt.
     *
     * @param dateTimeString der ISO-8601-konforme String
     * @return das geparste LocalDateTime
     * @throws DateTimeParseException wenn der String nicht dem Format entspricht
     */
    public static LocalDateTime parseIsoLocalDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            throw new IllegalArgumentException("dateTimeString darf nicht null oder leer sein");
        }
        return LocalDateTime.parse(dateTimeString, ISO_FORMATTER);
    }

    /**
     * Gibt die aktuelle UTC-Zeit als LocalDateTime zurück.
     *
     * @return aktuelles Datum und Zeit in UTC
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(Clock.systemUTC());
    }
}