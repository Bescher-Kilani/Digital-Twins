package org.DigiTwinStudio.DigiTwin_Backend.exceptions;

public class ValidationException extends RuntimeException {

    /**
     * Constructs a new ValidationException with a descriptive message.
     *
     * @param message the detail message
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new ValidationException with a descriptive message.
     *
     * @param message the detail message
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
