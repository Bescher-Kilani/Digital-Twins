package org.DigiTwinStudio.DigiTwin_Backend.services;

import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.UploadResponseDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.FileStorageException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ValidationException;
import org.DigiTwinStudio.DigiTwin_Backend.domain.UploadedFile;
import org.DigiTwinStudio.DigiTwin_Backend.services.FileStorageService;
import org.DigiTwinStudio.DigiTwin_Backend.validation.FileUploadValidator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PropertyFileUploadService {

    private final FileStorageService fileStorageService;
    private final FileUploadValidator fileUploadValidator;

    /**
     * Uploads and stores a property file (e.g., PDF, image) used in a submodel.
     * The file is validated and saved, and metadata is returned to the client.
     *
     * @param file the uploaded MultipartFile from the HTTP request
     * @return UploadResponseDto containing file metadata
     * @throws ValidationException      if file type, size, or name is invalid
     * @throws FileStorageException     if storage fails (I/O, permissions, etc.)
     */
    public UploadResponseDto uploadFile(MultipartFile file) {

        // 1. Validate the file (throws ValidationException on failure)
        fileUploadValidator.validate(file);

        // 2. Save the file and associate it with the current user
        UploadedFile savedFile = fileStorageService.store(file, getCurrentUserId());

        // 3. Build response DTO with metadata
        return UploadResponseDto.builder()
                .fileId(savedFile.getId())
                .filename(savedFile.getFilename())
                .contentType(savedFile.getContentType())
                .size(savedFile.getSize())
                .build();
    }

    /**
     * Deletes a previously uploaded file if it belongs to the current user.
     *
     * @param fileId the ID of the file to delete
     * @throws FileStorageException if the file cannot be deleted or access is denied
     */
    public void deleteFile(String fileId) {
        fileStorageService.delete(fileId, getCurrentUserId());
    }

    /**
     * Retrieves the ID of the currently authenticated user.
     * This is required to associate files with users and enforce access control.
     *
     * @return the current user ID (e.g., from JWT token's `sub` claim)
     */
    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
        // Note: this returns the `sub` field if Keycloak is configured properly
    }
}
