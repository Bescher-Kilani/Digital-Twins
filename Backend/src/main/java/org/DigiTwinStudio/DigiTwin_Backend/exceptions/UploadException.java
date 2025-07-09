package org.DigiTwinStudio.DigiTwin_Backend.exceptions;

public class UploadException extends RuntimeException {

    /**
     * Constructs a new UploadException with the specified detail message.
     *
     * @param message the detail message describing the reason for the exception
     */
    public UploadException(String message) {
        super(message);
    }

    /**
     * Constructs a new UploadException with the specified detail message and cause.
     *
     * @param message the detail message describing the reason for the exception
     * @param cause the cause of the exception, which can be retrieved later by the {@link Throwable#getCause()} method
     */
    public UploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
