package org.DigiTwinStudio.DigiTwin_Backend.utils;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Contains Help-Methods to assist in working with dates and timestamps.
 * As Spring Component can be used anywhere.
 */
@Component
public class DateTimeUtil {

    /**
     * Returns current UTC-time as LocalDateTime-Object
     *
     * @return current date and time in UTC
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(Clock.systemUTC());
    }
}
