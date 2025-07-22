package org.DigiTwinStudio.DigiTwin_Backend.exceptions;

import org.springframework.http.HttpStatus;

public class ConflictException extends ValidationException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
