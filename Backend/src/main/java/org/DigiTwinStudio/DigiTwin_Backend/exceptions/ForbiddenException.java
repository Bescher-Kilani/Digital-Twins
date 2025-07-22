package org.DigiTwinStudio.DigiTwin_Backend.exceptions;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ValidationException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
