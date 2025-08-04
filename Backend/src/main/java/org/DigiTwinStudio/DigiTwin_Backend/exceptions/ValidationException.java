package org.DigiTwinStudio.DigiTwin_Backend.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Base exception for validation errors with HTTP status support.
 */
public abstract class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns the associated HTTP status for this validation error.
     *
     * @return the HTTP status
     */
    public abstract HttpStatus getHttpStatus();
}
