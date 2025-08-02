package org.DigiTwinStudio.DigiTwin_Backend.domain;

public record ExportedFile(byte[] bytes, String filename, String contentType) {
}