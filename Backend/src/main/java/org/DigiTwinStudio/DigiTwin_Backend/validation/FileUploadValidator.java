package org.DigiTwinStudio.DigiTwin_Backend.validation;

import java.util.Map;

import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ValidationException;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.Locale;


/**
 * Component responsible for validating uploaded files
 * (e.g. PDFs, PNGs, JSON, AASX) against project-specific rules.
 */
@Component
public class FileUploadValidator {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final Map<String, String> EXT_TO_MIME = Map.of(
            "pdf",  "application/pdf",
            "png",  "image/png",
            "json", "application/json",
            "aasx", "application/aasx+zip"
    );

    /**
     * Checks the uploaded file for presence, size, extension, and MIME type.
     *
     * @param file the uploaded file
     * @throws ValidationException if validation fails
     */
    public void validate(MultipartFile file) throws ValidationException {
        // 1. Check presence
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file must not be empty");
        }
        // 2. Check size
        long size = file.getSize();
        if (size > MAX_FILE_SIZE) {
            double sizeMb = size / 1024.0 / 1024.0;
            double maxMb  = MAX_FILE_SIZE / 1024.0 / 1024.0;
            throw new BadRequestException(
                    String.format(Locale.US,"File size (%.2f MB) exceeds maximum of %.2f MB", sizeMb, maxMb)
            );
        }
        // 3. Determine MIME type
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BadRequestException("Could not determine file MIME type");
        }
        // 4. Extract and validate file extension
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            throw new BadRequestException("Original filename must not be blank and must contain an extension");
        }

        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        // 5. Look up the allowed MIME type for this extension
        String allowedMime = EXT_TO_MIME.get(ext);
        if (allowedMime == null) {
            throw new BadRequestException("Unsupported file extension: ." + ext);
        }
        // 6. Compare actual contentType with expected
        if (!allowedMime.equals(contentType)) {
            throw new BadRequestException(
                    String.format("File content type '%s' does not match expected MIME type for '.%s'", contentType, ext)
            );
        }
    }
}
