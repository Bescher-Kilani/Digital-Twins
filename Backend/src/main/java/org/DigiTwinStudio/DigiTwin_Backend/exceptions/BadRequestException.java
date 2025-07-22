package org.DigiTwinStudio.DigiTwin_Backend.exceptions;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ValidationException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
