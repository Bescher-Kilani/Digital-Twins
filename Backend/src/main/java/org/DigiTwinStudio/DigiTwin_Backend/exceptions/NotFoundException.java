package org.DigiTwinStudio.DigiTwin_Backend.exceptions;

public class NotFoundException extends RuntimeException {

    /**
     * Constructs a new NotFoundException with a detail message.
     *
     * @param message the detail message
     */
    public NotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new NotFoundException with a message and underlying cause.
     *
     * @param message the detail message
     * @param cause   the root exception
     */
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
