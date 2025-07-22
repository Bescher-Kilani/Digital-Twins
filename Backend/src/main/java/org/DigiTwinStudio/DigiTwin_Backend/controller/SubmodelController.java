package org.DigiTwinStudio.DigiTwin_Backend.controller;

import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.SubmodelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.TemplateDto;
import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelService;
import org.DigiTwinStudio.DigiTwin_Backend.services.SubmodelService;
import org.DigiTwinStudio.DigiTwin_Backend.services.TemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubmodelController {

    private final SubmodelService submodelService;
    private final TemplateService templateService;
    private final AASModelService aasModelService;

    /**
     * TODO: replace @RequestHeader("userId") with
     * @AuthenticationPrincipal Jwt jwt
     */


    /**
     * Returns a list of all available submodel templates.
     * These can be used to create new submodels.
     */
    @GetMapping("/submodels/templates")
    public ResponseEntity<List<TemplateDto>> listAvailableTemplates() {
        return ResponseEntity.ok(templateService.getAvailableTemplates());
    }

    /**
     * Creates a new empty submodel based on a selected template.
     * @param templateId ID of the template to base the submodel on
     */
    @GetMapping("/submodels/new")
    public ResponseEntity<SubmodelDto> getNewSubmodel(@RequestParam String templateId) {
        return ResponseEntity.ok(submodelService.createEmptySubmodelFromTemplate(templateId));
    }

    /**
     * Adds a submodel to an existing AAS model.
     * @param modelId ID of the target AAS model
     * @param userId ID of the user performing the operation (from header)
     * @param dto Submodel data to attach
     */
    @PostMapping("/models/{modelId}/submodels")
    public ResponseEntity<AASModelDto> addSubmodelToModel(
            @PathVariable String modelId,
            @RequestHeader("userId") String userId,
            @RequestBody SubmodelDto dto) throws ValidationException {
        return ResponseEntity.ok(aasModelService.attachSubmodel(modelId, dto, userId));
    }

    /**
     * Returns a specific submodel by its ID from a given AAS model.
     * @param modelId ID of the AAS model
     * @param submodelId ID of the submodel to retrieve
     */
    @GetMapping("/models/{modelId}/submodels/{submodelId}")
    public ResponseEntity<SubmodelDto> getSubmodel(
            @PathVariable String modelId,
            @PathVariable String submodelId) {
        return ResponseEntity.ok(submodelService.getSubmodel(modelId, submodelId));
    }

    /**
     * Updates an existing submodel inside a given AAS model.
     * @param modelId ID of the parent AAS model
     * @param submodelId ID of the submodel to update
     * @param userId user who performs the update
     * @param dto updated submodel data
     */
    @PutMapping("/models/{modelId}/submodels/{submodelId}")
    public ResponseEntity<AASModelDto> updateSubmodel(
            @PathVariable String modelId,
            @PathVariable String submodelId,
            @RequestHeader("userId") String userId,
            @RequestBody SubmodelDto dto) throws ValidationException {
        return ResponseEntity.ok(aasModelService.updateSubmodel(modelId, submodelId, dto, userId));
    }

    /**
     * Removes a submodel from the specified AAS model.
     * @param modelId ID of the parent AAS model
     * @param submodelId ID of the submodel to remove
     * @param userId ID of the requesting user
     */
    @DeleteMapping("/models/{modelId}/submodels/{submodelId}")
    public ResponseEntity<AASModelDto> removeSubmodelFromModel(
            @PathVariable String modelId,
            @PathVariable String submodelId,
            @RequestHeader("userId") String userId) throws ValidationException {
        return ResponseEntity.ok(aasModelService.removeSubmodel(modelId, submodelId, userId));
    }
}
