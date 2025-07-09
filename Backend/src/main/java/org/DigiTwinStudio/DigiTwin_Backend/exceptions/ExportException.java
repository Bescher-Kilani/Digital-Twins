package org.DigiTwinStudio.DigiTwin_Backend.exceptions;

public class ExportException extends RuntimeException {

    /**
     * Constructs a new ExportException with the given message.
     *
     * @param message the detail message
     */
    public ExportException(String message) {
        super(message);
    }

    /**
     * Constructs a new ExportException with the given message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
