package org.DigiTwinStudio.DigiTwin_Backend.controller;

import lombok.RequiredArgsConstructor;

import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.PublishRequestDto;

import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


import java.util.List;

@RestController
@RequestMapping("/models")
@RequiredArgsConstructor
public class AASModelController {

    private final AASModelService aasModelService;

    /**
     * Returns a list of all AAS models owned by the authenticated user.
     *
     * @param jwt the authentication token (to extract user id)
     * @return list of AASModelDto objects (200 OK)
     */
    @GetMapping
    public ResponseEntity<List<AASModelDto>> listAASModels(@AuthenticationPrincipal Jwt jwt) {
        List<AASModelDto> models = aasModelService.getAllForUser(jwt.getSubject());
        return ResponseEntity.ok(models);
    }

    /**
     * Retrieves a specific AAS model by its id if the user is the owner.
     *
     * @param id  the id of the model
     * @param jwt the authentication token (to extract user id)
     * @return the requested AASModelDto (200 OK), or 404/403 if not found/not permitted
     */
    @GetMapping("/{id}")
    public ResponseEntity<AASModelDto> getAASModel(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        AASModelDto model = aasModelService.getById(id, jwt.getSubject());
        return ResponseEntity.ok(model);
    }

    /**
     * Creates a new AAS model for the authenticated user.
     *
     * @param jwt the authentication token (to extract user id)
     * @param dto the model details to create
     * @return the created AASModelDto (200 OK)
     */
    @PostMapping("/new")
    public ResponseEntity<AASModelDto> createNewModel(@AuthenticationPrincipal Jwt jwt, @RequestBody AASModelDto dto) {
        AASModelDto saved = aasModelService.createModel(jwt.getSubject(), dto);
        return ResponseEntity.ok(saved);
    }

    /**
     * Updates and saves an existing AAS model owned by the authenticated user.
     *
     * @param id  the id of the model to update
     * @param dto the updated model data
     * @param jwt the authentication token (to extract user id)
     * @return the saved AASModelDto (200 OK)
     */
    @PutMapping("/{id}/save")
    public ResponseEntity<AASModelDto> saveAASModel(@PathVariable String id, @RequestBody AASModelDto dto, @AuthenticationPrincipal Jwt jwt) {
        AASModelDto saved = aasModelService.saveModel(id, jwt.getSubject(), dto);
        return ResponseEntity.ok(saved);
    }

    /**
     * Deletes an AAS model by its id if the user is the owner.
     *
     * @param id  the id of the model to delete
     * @param jwt the authentication token (to extract user id)
     * @return 204 No Content if successful, 404/403 otherwise
     */
    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Void> deleteAASModel(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        aasModelService.deleteModel(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    /**
     * Publishes an AAS model to the marketplace if the user is the owner.
     *
     * @param id      the id of the model to publish
     * @param request the publish metadata (author, description, tags)
     * @param jwt     the authentication token (to extract user id)
     * @return 200 OK if published, or 400/404/409 if errors occur
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publishAASModel(@PathVariable String id, @RequestBody PublishRequestDto request, @AuthenticationPrincipal Jwt jwt) {
        aasModelService.publishModel(id, jwt.getSubject(), request);
        return ResponseEntity.ok().build();
    }

    /**
     * Unpublishes an AAS model from the marketplace if the user is the owner.
     *
     * @param id  the id of the model to unpublish
     * @param jwt the authentication token (to extract user id)
     * @return 200 OK if unpublished, or 404/403 otherwise
     */
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<Void> unpublishAASModel(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        this.aasModelService.unpublishModel(id, jwt.getSubject());
        return ResponseEntity.ok().build();
    }


}
