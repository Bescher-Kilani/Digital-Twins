package org.DigiTwinStudio.DigiTwin_Backend.exceptions;

/**
 * Thrown when a file storage operation fails.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
