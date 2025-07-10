package org.DigiTwinStudio.DigiTwin_Backend.validation;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ValidationException;

/**
 * Component responsible for validating uploaded files
 * (e.g. PDFs, PNGs, JSON, AASX) against project-specific rules.
 */
@Component
public class FileUploadValidator {

    /** Maximum allowed file size in bytes (10 MB) */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * Mapping from file extension to its single allowed MIME type.
     * E.g. "pdf" → "application/pdf"
     */
    private static final Map<String, String> EXT_TO_MIME = Map.of(
            "pdf",  "application/pdf",
            "png",  "image/png",
            "json", "application/json",
            "aasx", "application/aasx+zip"
    );

    /**
     * Validate the given MultipartFile according to:
     * 1. File must not be null or empty.
     * 2. File size must not exceed MAX_FILE_SIZE.
     * 3. File must have an original filename with an extension.
     * 4. Extension must be supported.
     * 5. Detected contentType must exactly match the expected MIME type for that extension.
     *
     * @param file the uploaded file to validate
     * @throws ValidationException if any validation rule is violated
     */
    public void validate(MultipartFile file) throws ValidationException {
        // 1. Check presence
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Uploaded file must not be empty");
        }
        // 2. Check size
        if (file.getSize() > MAX_FILE_SIZE) {
            double sizeMb = file.getSize() / 1024.0 / 1024.0;
            double maxMb  = MAX_FILE_SIZE / 1024.0 / 1024.0;
            throw new ValidationException(
                    String.format("File size (%.2f MB) exceeds maximum of %.2f MB", sizeMb, maxMb)
            );
        }
        // 3. Determine MIME type
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new ValidationException("Could not determine file MIME type");
        }
        // 4. Extract and validate file extension
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            throw new ValidationException("Original filename must not be blank and must contain an extension");
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        // 5. Look up the allowed MIME type for this extension
        String allowedMime = EXT_TO_MIME.get(ext);
        if (allowedMime == null) {
            throw new ValidationException("Unsupported file extension: ." + ext);
        }
        // 6. Compare actual contentType with expected
        if (!allowedMime.equals(contentType)) {
            throw new ValidationException(
                    String.format("File content type '%s' does not match expected MIME type for '.%s'", contentType, ext)
            );
        }
    }
}
