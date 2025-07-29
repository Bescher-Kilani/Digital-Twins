package org.DigiTwinStudio.DigiTwin_Backend.controller;

import lombok.RequiredArgsConstructor;

import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.PublishRequestDto;

import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


import java.util.List;

@RestController
@RequestMapping("/models")
@RequiredArgsConstructor
public class AASModelController {

    private final AASModelService aasModelService;

    @GetMapping
    public ResponseEntity<List<AASModelDto>> listAASModels(@AuthenticationPrincipal Jwt jwt) {
        List<AASModelDto> models = aasModelService.getAllForUser(jwt.getSubject());
        return ResponseEntity.ok(models);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AASModelDto> getAASModel(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        AASModelDto model = aasModelService.getById(id, jwt.getSubject());
        return ResponseEntity.ok(model);
    }

    @PostMapping("/new")
    public ResponseEntity<AASModelDto> createNewModel(@AuthenticationPrincipal Jwt jwt, @RequestBody AASModelDto dto) {
        AASModelDto saved = aasModelService.createModel(jwt.getSubject(), dto);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/save")
    public ResponseEntity<AASModelDto> saveAASModel(@PathVariable String id, @RequestBody AASModelDto dto, @AuthenticationPrincipal Jwt jwt) {
        AASModelDto saved = aasModelService.saveModel(id, jwt.getSubject(), dto);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Void> deleteAASModel(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        aasModelService.deleteModel(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publishAASModel(@PathVariable String id, @RequestBody PublishRequestDto request, @AuthenticationPrincipal Jwt jwt) {
        aasModelService.publishModel(id, jwt.getSubject(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<Void> unpublishAASModel(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        this.aasModelService.unpublishModel(id, jwt.getSubject());
        return ResponseEntity.ok().build();
    }


}
