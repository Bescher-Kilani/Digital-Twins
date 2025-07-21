package org.DigiTwinStudio.DigiTwin_Backend.controller;

import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class AASModelController {

    private final AASModelService aasModelService;

    /**
     * retrieves all AAS-models of the given user
     * @param userId id of user
     * @return list of users' models
     */
    @GetMapping
    public ResponseEntity<List<AASModelDto>> listAASModels(@RequestHeader("userId") String userId) {
        return ResponseEntity.ok(aasModelService.getAllForUser(userId));
    }

    /**
     * return the AAS model requested if it exists and user has access.
     * @param modelId id of a model
     * @param userId userId
     * @return modelDTO or Response with error if access denied or model not found
     */
    @GetMapping("/{modelId}")
    public ResponseEntity<?> getAASModel(@PathVariable String modelId, @RequestHeader("userId") String userId) {
        try {
            AASModelDto dto = this.aasModelService.getById(modelId, userId);
            return ResponseEntity.ok(dto); // HTTP 200 OK mit DTO
        } catch (ResponseStatusException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getReason()); // "Model not found" oder "Access denied"
        }
    }


}
