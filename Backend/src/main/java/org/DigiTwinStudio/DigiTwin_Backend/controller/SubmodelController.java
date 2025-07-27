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

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class SubmodelController {

    private final SubmodelService submodelService;
    private final TemplateService templateService;
    private final AASModelService aasModelService;

    @GetMapping("/submodels/templates")
    public ResponseEntity<List<TemplateDto>> listAvailableSubmodelTemplates() {
        List<TemplateDto> templates = templateService.getAvailableTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/submodels/new")
    public ResponseEntity<SubmodelDto> getNewSubmodel(@RequestParam String templateId) {
        SubmodelDto dto = submodelService.createEmptySubmodelFromTemplate(templateId);
        return ResponseEntity.ok(dto);
    }

    // forbidden and conflict exception used but not bad request exception
    @PostMapping("/models/{modelId}/submodels")
    public ResponseEntity<AASModelDto> addSubmodelToModel(@PathVariable String modelId, @RequestBody SubmodelDto dto, @AuthenticationPrincipal Jwt jwt) {
        AASModelDto updated = aasModelService.attachSubmodel(modelId, dto, jwt.getSubject());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/models/{modelId}/submodels/{submodelId}")
    public ResponseEntity<SubmodelDto> getSubmodel(@PathVariable String modelId, @PathVariable String submodelId) {
        SubmodelDto dto = submodelService.getSubmodel(modelId, submodelId);
        return ResponseEntity.ok(dto);
    }
    // forbidden exception used but not bad request exception
    @PutMapping("/models/{modelId}/submodels/{submodelId}")
    public ResponseEntity<AASModelDto> updateSubmodel( @PathVariable String modelId, @PathVariable String submodelId, @RequestBody SubmodelDto dto, @AuthenticationPrincipal Jwt jwt) {
        AASModelDto updated = aasModelService.updateSubmodel(modelId, submodelId, dto, jwt.getSubject());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/models/{modelId}/submodels/{submodelId}")
    public ResponseEntity<AASModelDto> removeSubmodelFromModel( @PathVariable String modelId, @PathVariable String submodelId, @AuthenticationPrincipal Jwt jwt) {
        aasModelService.removeSubmodel(modelId, submodelId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
