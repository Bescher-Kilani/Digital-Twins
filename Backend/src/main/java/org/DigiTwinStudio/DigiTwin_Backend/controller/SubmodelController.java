package org.DigiTwinStudio.DigiTwin_Backend.controller;

import lombok.RequiredArgsConstructor;

import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.SubmodelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.TemplateDto;
import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelService;
import org.DigiTwinStudio.DigiTwin_Backend.services.SubmodelService;
import org.DigiTwinStudio.DigiTwin_Backend.services.TemplateService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for submodel operations and template access.
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class SubmodelController {

    private final SubmodelService submodelService;
    private final TemplateService templateService;
    private final AASModelService aasModelService;

    /**
     * Retrieves a list of all available submodel templates.
     *
     * @return a list of TemplateDto representing available submodel templates
     */
    @GetMapping("/submodels/templates")
    public ResponseEntity<List<TemplateDto>> listAvailableSubmodelTemplates() {
        List<TemplateDto> templates = templateService.getAvailableTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * Creates a new empty submodel from the given template ID.
     *
     * @param templateId the ID of the template to use
     * @return the newly created SubmodelDto
     */
    @GetMapping("/submodels/new")
    public ResponseEntity<SubmodelDto> getNewSubmodel(@RequestParam String templateId) {
        SubmodelDto dto = submodelService.createEmptySubmodelFromTemplate(templateId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Attaches a submodel to an existing AAS model.
     * May throw ForbiddenException or ConflictException.
     *
     * @param modelId the ID of the AAS model
     * @param dto the submodel to attach
     * @param jwt the JWT token of the authenticated user
     * @return the updated AASModelDto
     */
    @PostMapping("/models/{modelId}/submodels")
    public ResponseEntity<AASModelDto> addSubmodelToModel(@PathVariable String modelId, @RequestBody SubmodelDto dto, @AuthenticationPrincipal Jwt jwt) {
        AASModelDto updated = aasModelService.attachSubmodel(modelId, dto, jwt.getSubject());
        return ResponseEntity.ok(updated);
    }

    /**
     * Retrieves a specific submodel by model ID and submodel ID.
     *
     * @param modelId the ID of the AAS model
     * @param submodelId the ID of the submodel to retrieve
     * @return the requested SubmodelDto
     */
    @GetMapping("/models/{modelId}/submodels/{submodelId}")
    public ResponseEntity<SubmodelDto> getSubmodel(@PathVariable String modelId, @PathVariable String submodelId) {
        SubmodelDto dto = submodelService.getSubmodel(modelId, submodelId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Updates an existing submodel in an AAS model.
     * May throw ForbiddenException or ConflictException.
     *
     * @param modelId the ID of the AAS model
     * @param submodelId the ID of the submodel to update
     * @param dto the updated submodel
     * @param jwt the JWT token of the authenticated user
     * @return the updated AASModelDto
     */
    @PutMapping("/models/{modelId}/submodels/{submodelId}")
    public ResponseEntity<AASModelDto> updateSubmodel(@PathVariable String modelId, @PathVariable String submodelId, @RequestBody SubmodelDto dto, @AuthenticationPrincipal Jwt jwt) {
        AASModelDto updated = aasModelService.updateSubmodel(modelId, submodelId, dto, jwt.getSubject());
        return ResponseEntity.ok(updated);
    }

    /**
     * Removes a submodel from the specified AAS model.
     *
     * @param modelId the ID of the AAS model
     * @param submodelId the ID of the submodel to remove
     * @param jwt the JWT token of the authenticated user
     * @return HTTP 204 No Content if removal was successful
     */
    @DeleteMapping("/models/{modelId}/submodels/{submodelId}")
    public ResponseEntity<AASModelDto> removeSubmodelFromModel(@PathVariable String modelId, @PathVariable String submodelId, @AuthenticationPrincipal Jwt jwt) {
        aasModelService.removeSubmodel(modelId, submodelId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
