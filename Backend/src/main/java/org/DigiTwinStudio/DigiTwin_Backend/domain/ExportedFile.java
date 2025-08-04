package org.DigiTwinStudio.DigiTwin_Backend.domain;

/**
 * Represents a file to be exported, including its bytes, filename, and content type.
 */
public record ExportedFile(byte[] bytes, String filename, String contentType) {
}
