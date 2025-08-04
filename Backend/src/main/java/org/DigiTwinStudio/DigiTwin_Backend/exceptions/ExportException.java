package org.DigiTwinStudio.DigiTwin_Backend.exceptions;

/**
 * Thrown when an export operation fails.
 */
public class ExportException extends RuntimeException {

    public ExportException(String message) {
        super(message);
    }

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
