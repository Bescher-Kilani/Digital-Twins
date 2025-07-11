package org.DigiTwinStudio.DigiTwin_Backend.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;


import java.time.Instant;
import java.util.Map;

public class GlobalExceptionHandler extends RuntimeException {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation-related exceptions caused by bad user input or model issues.
     *
     * @param ex the thrown ValidationException
     * @return 400 Bad Request with error message and timestamp
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidationException(ValidationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles cases where a requested resource (e.g., model, template) is not found.
     *
     * @param ex the thrown NotFoundException
     * @return 404 Not Found with error message and timestamp
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFoundException(NotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles upload-related issues such as malformed files or backend errors.
     * Chooses 400 or 500 based on the underlying cause type.
     *
     * @param ex the thrown UploadException
     * @return HTTP error response with dynamic status and message
     */
    @ExceptionHandler(UploadException.class)
    public ResponseEntity<Object> handleUploadException(UploadException ex) {
        HttpStatus status = ex.getCause() instanceof RuntimeException
                ? HttpStatus.INTERNAL_SERVER_ERROR
                : HttpStatus.BAD_REQUEST;
        return buildResponse(status, ex.getMessage());
    }

    /**
     * Handles serialization or export errors related to AASX generation.
     *
     * @param ex the thrown ExportException
     * @return 500 Internal Server Error with error message and timestamp
     */
    @ExceptionHandler(ExportException.class)
    public ResponseEntity<Object> handleExportException(ExportException ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    /**
     * Handles file storage failures such as access, IO, or GridFS errors.
     *
     * @param ex the thrown FileStorageException
     * @return 500 Internal Server Error with error message and timestamp
     */
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Object> handleFileStorageException(FileStorageException ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    /**
     * Fallback for any unhandled exceptions in the application.
     * Logs the full stack trace for internal investigation while returning a safe generic message to the client.
     *
     * @param ex the thrown unexpected exception
     * @return 500 Internal Server Error with generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex) {
        logger.error("Unhandled exception occurred", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<Object> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = Map.of(
                "error", message,
                "status", status.value(),
                "timestamp", Instant.now().toString()
        );
        return ResponseEntity.status(status).body(body);
    }
}
