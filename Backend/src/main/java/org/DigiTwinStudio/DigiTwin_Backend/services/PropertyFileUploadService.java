package org.DigiTwinStudio.DigiTwin_Backend.services;

import lombok.RequiredArgsConstructor;

import org.DigiTwinStudio.DigiTwin_Backend.dtos.UploadResponseDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.FileStorageException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ValidationException;
import org.DigiTwinStudio.DigiTwin_Backend.domain.UploadedFile;
import org.DigiTwinStudio.DigiTwin_Backend.validation.FileUploadValidator;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles validation and upload of property files for models.
 */
@Service
@RequiredArgsConstructor
public class PropertyFileUploadService {

    private final FileStorageService fileStorageService;
    private final FileUploadValidator fileUploadValidator;

    /**
     * Validates and uploads a file for a specific user and model, then returns metadata about the uploaded file.
     *
     * @param file    the file to upload (e.g., PDF, image)
     * @param modelId the ID of the model or submodel
     * @param userId  the ID of the user uploading the file
     * @return        metadata about the uploaded file
     * @throws ValidationException   if the file is invalid
     * @throws FileStorageException  if storing the file fails
     */
    public UploadResponseDto uploadFile(MultipartFile file, String modelId, String userId) {
        fileUploadValidator.validate(file);
        UploadedFile savedFile = fileStorageService.store(file, userId, modelId);

        return UploadResponseDto.builder()
                .fileId(savedFile.getId())
                .filename(savedFile.getFilename())
                .contentType(savedFile.getContentType())
                .size(savedFile.getSize())
                .uploadedAt(savedFile.getUploadedAt())
                .build();
    }


    /**
     * Deletes an uploaded file if it belongs to the specified user.
     *
     * @param fileId the ID of the file to delete
     * @param userId the ID of the user
     * @throws FileStorageException if the file does not exist, cannot be deleted, or access is denied
     */
    public void deleteFile(String fileId, String userId) {
        fileStorageService.delete(fileId, userId);
    }
}
