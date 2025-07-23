package org.DigiTwinStudio.DigiTwin_Backend.services;

import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.UploadResponseDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.FileStorageException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ValidationException;
import org.DigiTwinStudio.DigiTwin_Backend.domain.UploadedFile;
import org.DigiTwinStudio.DigiTwin_Backend.services.FileStorageService;
import org.DigiTwinStudio.DigiTwin_Backend.validation.FileUploadValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PropertyFileUploadService {

    private final FileStorageService fileStorageService;
    private final FileUploadValidator fileUploadValidator;

    /**
     * Uploads and stores a property file (e.g., PDF, image) used in a submodel.
     * The file is validated, associated with the requesting user and model, saved to storage,
     * and metadata is returned to the client.
     *
     * @param file     the uploaded file (e.g. PDF, image) from the HTTP request
     * @param modelId  the ID of the model or submodel to which the file belongs
     * @param userId   the ID of the currently authenticated user (typically from JWT or Principal)
     * @return         an {@link UploadResponseDto} containing file metadata such as ID, name, type, and timestamp
     *
     * @throws ValidationException   if the file is invalid (wrong type, empty, too large, etc.)
     * @throws FileStorageException  if storing the file fails due to I/O errors, permission issues, etc.
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
     * Deletes a previously uploaded file if it belongs to the specified user.
     * The method checks ownership before performing the deletion.
     *
     * @param fileId the ID of the file to delete
     * @param userId the ID of the currently authenticated user
     *
     * @throws FileStorageException if the file does not exist, cannot be deleted, or access is denied
     */
    public void deleteFile(String fileId, String userId) {
        fileStorageService.delete(fileId, userId);
    }



}
