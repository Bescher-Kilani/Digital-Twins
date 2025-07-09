package org.DigiTwinStudio.DigiTwin_Backend.exceptions;

public class ExportException extends RuntimeException {

    /**
     * Constructs a new ExportException with the specified detail message.
     *
     * @param message the detail message describing the reason for the exception
     */
    public ExportException(String message) {
        super(message);
    }

    /**
     * Constructs a new ExportException with the specified detail message and cause.
     *
     * @param message the detail message describing the reason for the exception
     * @param cause the cause of the exception (which is saved for later retrieval by the {@link Throwable#getCause()} method)
     */
    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
