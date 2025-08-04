package org.DigiTwinStudio.DigiTwin_Backend.controller;

import lombok.RequiredArgsConstructor;

import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportFormat;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportedFile;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.SubmodelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.TemplateDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.UploadResponseDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ExportException;
import org.DigiTwinStudio.DigiTwin_Backend.services.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for guest operations on AAS models, submodels, and file uploads.
 */
@RestController
@RequestMapping("/guest")
@RequiredArgsConstructor
public class GuestController {

    private final SubmodelService submodelService;
    private final TemplateService templateService;
    private final AASModelService aasModelService;
    private final ExportService exportService;
    private final PropertyFileUploadService propertyFileUploadService;

    /**
     * Returns all available submodel templates for guests.
     *
     * @return list of available templates
     */
    @GetMapping("/submodels/templates")
    public ResponseEntity<List<TemplateDto>> listAvailableSubmodelTemplates() {
        List<TemplateDto> templates = templateService.getAvailableTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * Creates an empty submodel based on a template ID.
     * Used to preview or test submodels as a guest.
     *
     * @param templateId the ID of the template to use
     * @return empty submodel based on the template
     */
    @GetMapping("/submodels/new")
    public ResponseEntity<SubmodelDto> getNewSubmodel(@RequestParam String templateId) {
        SubmodelDto dto = submodelService.createEmptySubmodelFromTemplate(templateId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Attaches a submodel to an existing AAS model.
     * The user is fixed as "GUEST" for audit purposes.
     *
     * @param modelId the ID of the AAS model
     * @param dto     the submodel data to attach
     * @return the updated AAS model
     */
    @PostMapping("/models/{modelId}/submodels")
    public ResponseEntity<AASModelDto> addSubmodelToModel(
            @PathVariable String modelId,
            @RequestBody SubmodelDto dto) {
        AASModelDto updated = aasModelService.attachSubmodel(modelId, dto, "GUEST");
        return ResponseEntity.ok(updated);
    }

    /**
     * Retrieves a specific submodel from a given AAS model.
     *
     * @param modelId    the ID of the AAS model
     * @param submodelId the ID of the submodel
     * @return the requested submodel
     */
    @GetMapping("/models/{modelId}/submodels/{submodelId}")
    public ResponseEntity<SubmodelDto> getSubmodel(
            @PathVariable String modelId,
            @PathVariable String submodelId) {
        SubmodelDto dto = submodelService.getSubmodel(modelId, submodelId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Updates an existing submodel in a model.
     * This version assumes the update is done by a guest user.
     *
     * @param modelId    the ID of the AAS model
     * @param submodelId the ID of the submodel
     * @param dto        the updated submodel data
     * @return the updated AAS model
     */
    @PutMapping("/models/{modelId}/submodels/{submodelId}")
    public ResponseEntity<AASModelDto> updateSubmodel(
            @PathVariable String modelId,
            @PathVariable String submodelId,
            @RequestBody SubmodelDto dto) {
        AASModelDto updated = aasModelService.updateSubmodel(modelId, submodelId, dto, "GUEST");
        return ResponseEntity.ok(updated);
    }

    /**
     * Removes a submodel from an AAS model.
     * Guest actions are labeled with "GUEST".
     *
     * @param modelId    the ID of the AAS model
     * @param submodelId the ID of the submodel to remove
     * @return HTTP 204 No Content if successful
     */
    @DeleteMapping("/models/{modelId}/submodels/{submodelId}")
    public ResponseEntity<AASModelDto> removeSubmodelFromModel(
            @PathVariable String modelId,
            @PathVariable String submodelId) {
        aasModelService.removeSubmodel(modelId, submodelId, "GUEST");
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates a new empty AAS model for a guest user.
     * Guest user is identified as "GUEST".
     *
     * @return the empty model
     */
    @PostMapping("/models/new")
    public ResponseEntity<AASModelDto> createModelAsGuest(@RequestBody AASModelDto dto) {
        AASModelDto saved = aasModelService.createModel("GUEST", dto);
        return ResponseEntity.ok(saved);
    }

    /**
     * Saves the AAS model for a guest user.
     * Guest user is identified as "GUEST".
     *
     * @param id  the model ID
     * @param dto the model data to save
     * @return the saved model
     */
    @PutMapping("/models/{id}/save")
    public ResponseEntity<AASModelDto> saveAASModelAsGuest(
            @PathVariable String id,
            @RequestBody AASModelDto dto) {
        AASModelDto saved = aasModelService.saveModel(id, "GUEST", dto);
        return ResponseEntity.ok(saved);
    }

    /**
     * Uploads a property file (e.g., PDF, image) for a guest user.
     *
     * @param modelId the ID of the model to which the file belongs
     * @param file    the uploaded property file
     * @return metadata about the uploaded file
     */
    @PostMapping("/models/{modelId}/upload/property")
    public ResponseEntity<UploadResponseDto> uploadPropertyFileAsGuest(
            @PathVariable String modelId,
            @RequestParam("file") MultipartFile file) {

        UploadResponseDto response = propertyFileUploadService.uploadFile(file, modelId, "GUEST");
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a previously uploaded file for a guest user.
     *
     * @param fileId the ID of the file to delete
     * @return 204 No Content if successful
     */
    @DeleteMapping("/upload/{fileId}")
    public ResponseEntity<Void> deleteFileAsGuest(@PathVariable String fileId) {
        propertyFileUploadService.deleteFile(fileId, "GUEST");
        return ResponseEntity.noContent().build();
    }

    /**
     * Exports a stored guest AAS model by its ID in the specified format (e.g., JSON or AASX),
     * and returns it as a downloadable file in the HTTP response.
     *
     * @param id      the ID of the stored AAS model
     * @param name    the desired filename (without extension) for the exported file
     * @param format  the export format (e.g., JSON or AASX)
     * @return a {@link ResponseEntity} containing the model as a byte stream, download headers, and content type
     * @throws ExportException if the model cannot be exported
     */
    @GetMapping("/models/{id}/{name}/export/{format}")
    public ResponseEntity<byte[]> exportModel(
            @PathVariable String id,
            @PathVariable String name,
            @PathVariable ExportFormat format) {

        ExportedFile exported = exportService.export(id, name, format, "GUEST");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;" +
                        " filename=\"" + exported.filename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, exported.contentType())
                .body(exported.bytes());
    }
}
