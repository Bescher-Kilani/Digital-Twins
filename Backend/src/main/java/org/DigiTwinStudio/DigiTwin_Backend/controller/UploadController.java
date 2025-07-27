package org.DigiTwinStudio.DigiTwin_Backend.controller;

import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.UploadResponseDto;
import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelUploadService;
import org.DigiTwinStudio.DigiTwin_Backend.services.PropertyFileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



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
     * @param jwt the authenticated user
     * @return the parsed AAS model DTO
     */
    @PostMapping("/model")
    public ResponseEntity<AASModelDto> uploadModelFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject(); // Extract user ID from Principal
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
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        UploadResponseDto response = propertyFileUploadService.uploadFile(file, modelId, userId);
        return ResponseEntity.ok(response);
    }


    /**
     * Deletes an uploaded file by its ID.
     *
     * @param fileId ID of the file to delete
     * @param jwt the authenticated user
     * @return HTTP 204 if deletion was successful
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String fileId,
            @AuthenticationPrincipal Jwt jwt){
        propertyFileUploadService.deleteFile(fileId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

}
