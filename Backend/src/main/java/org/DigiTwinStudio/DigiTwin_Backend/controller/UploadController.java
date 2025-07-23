package org.DigiTwinStudio.DigiTwin_Backend.controller;

import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.UploadResponseDto;
import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelUploadService;
import org.DigiTwinStudio.DigiTwin_Backend.services.PropertyFileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

/**
 * REST controller for handling upload operations of AAS models and property files.
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final AASModelUploadService aasModelUploadService;
    private final PropertyFileUploadService propertyFileUploadService;

    /**
     * Uploads an AAS model file (.json or .aasx) and returns its parsed DTO.
     *
     * @param file the uploaded model file
     * @param principal the authenticated user principal
     * @return the parsed AAS model DTO
     */
    @PostMapping("/model")
    public ResponseEntity<AASModelDto> uploadModelFile(
            @RequestParam("file") MultipartFile file,
            Principal principal) {

        String userId = principal.getName(); // Extract user ID from Principal
        AASModelDto dto = aasModelUploadService.uploadAASModel(file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Uploads a file used as a property in a submodel (e.g. PDF, image).
     *
     * @param file the uploaded property file
     * @return metadata including the file ID
     */
    @PostMapping("/property")
    public ResponseEntity<UploadResponseDto> uploadPropertyFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("modelId") String modelId,
            Principal principal) {

        String userId = principal.getName();
        UploadResponseDto response = propertyFileUploadService.uploadFile(file, modelId, userId);
        return ResponseEntity.ok(response);
    }


    /**
     * Deletes an uploaded file by its ID.
     *
     * @param fileId ID of the file to delete
     * @param principal the authenticated user principal
     * @return HTTP 204 if deletion was successful
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String fileId,
            Principal principal) {
        propertyFileUploadService.deleteFile(fileId, principal.getName());
        return ResponseEntity.noContent().build();
    }

}
