package org.DigiTwinStudio.DigiTwin_Backend.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Thrown to indicate a 409 Conflict validation error.
 */
public class ConflictException extends ValidationException {

    public ConflictException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
