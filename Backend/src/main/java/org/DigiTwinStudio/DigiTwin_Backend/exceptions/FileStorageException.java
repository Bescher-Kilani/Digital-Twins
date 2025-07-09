package org.DigiTwinStudio.DigiTwin_Backend.exceptions;

public class FileStorageException extends RuntimeException {

    /**
     * Constructs a new FileStorageException with a detail message.
     *
     * @param message the error message
     */
    public FileStorageException(String message) {
        super(message);
    }

    /**
     * Constructs a new FileStorageException with a message and cause.
     *
     * @param message the error message
     * @param cause   the underlying exception
     */
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
