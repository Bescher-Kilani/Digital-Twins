package org.DigiTwinStudio.DigiTwin_Backend.controller;

import lombok.RequiredArgsConstructor;

import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.PublishRequestDto;

import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AASModelController {

    private final AASModelService aasModelService;

    @GetMapping
    public ResponseEntity<List<AASModelDto>> listAASModels(@AuthenticationPrincipal Principal principal) {
        List<AASModelDto> models = aasModelService.getAllForUser(principal.getName());
        return ResponseEntity.ok(models);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AASModelDto> getAASModel(@PathVariable String id, @AuthenticationPrincipal Principal principal) {
        AASModelDto model = aasModelService.getById(id, principal.getName());
        return ResponseEntity.ok(model);
    }

    @PostMapping("/new")
    public ResponseEntity<AASModelDto> getNewModel(@AuthenticationPrincipal Principal principal) {
        AASModelDto model = aasModelService.createEmpty(principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PutMapping("/{id}/save")
    public ResponseEntity<AASModelDto> saveAASModel(@PathVariable String id, @RequestBody AASModelDto dto, @AuthenticationPrincipal Principal principal) {
        AASModelDto saved = aasModelService.saveModel(id, principal.getName(), dto);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Void> deleteAASModel(@PathVariable String id, @AuthenticationPrincipal Principal principal) {
        aasModelService.deleteModel(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publishAASModel(@PathVariable String id, @RequestBody PublishRequestDto request, @AuthenticationPrincipal Principal principal) {
        aasModelService.publishModel(id, principal.getName(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<Void> unpublishAASModel(@PathVariable String id, @AuthenticationPrincipal Principal principal) {
        this.aasModelService.unpublishModel(id, principal.getName());
        return ResponseEntity.ok().build();
    }


}
