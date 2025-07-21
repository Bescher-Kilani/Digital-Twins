package org.DigiTwinStudio.DigiTwin_Backend.controller;

import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.PublishRequestDto;
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

    /**
     * fetch an empty model
     * @param userId id of user. can be "GUEST"
     * @return empty AASModel
     */
    @PostMapping("/new")
    public ResponseEntity<AASModelDto> getNewModel(@RequestHeader("userId") String userId) {
        return ResponseEntity.ok(this.aasModelService.createEmpty(userId));
    }

    /**
     * try to save or update the model.
     * @param modelId model id
     * @param userId id of user.
     * @param dto modelDto to be saved
     * @return ResponseEntity with the saved model, or an error if validation failed or model could not be found
     */
    @PutMapping("/{modelId}/save")
    public ResponseEntity<?> saveAASModel(@PathVariable String modelId,@RequestHeader("userId") String userId, @RequestBody AASModelDto dto) {
        try {
            return ResponseEntity.ok(this.aasModelService.saveModel(modelId, userId, dto));
        } catch (ValidationException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (ResponseStatusException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getReason());
        }
    }
    // ToDo: update vs save

    /**
     * delete model of user in a database
     * @param modelId id of a model to be deleted
     * @param userId userId
     * @return ResponseEntity with Status ok if deletion was successful, bad Request if model cannot be found or access denied if model id owner does not match userId
     */
    @DeleteMapping("/{modelId}/delete")
    public ResponseEntity<String> deleteAASModel(@PathVariable String modelId, @RequestHeader("userId") String userId) {
        try {
            this.aasModelService.deleteModel(modelId, userId);
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .body("Deleted " + modelId);
        } catch (ResponseStatusException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getReason());
        }
    }

    @PostMapping("/{modelId}/publish")
    public ResponseEntity<String> publishAASModel(@PathVariable String modelId, @RequestHeader("userId") String userId, @RequestBody PublishRequestDto request) {
        try {
            this.aasModelService.publishModel(modelId, userId, request);
            return ResponseEntity.ok("Published " + modelId);
        } catch (ValidationException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (ResponseStatusException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getReason());
        }
    }

}
