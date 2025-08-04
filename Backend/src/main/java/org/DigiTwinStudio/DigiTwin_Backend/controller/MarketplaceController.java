package org.DigiTwinStudio.DigiTwin_Backend.controller;

import lombok.RequiredArgsConstructor;

import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceEntryDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceSearchRequest;
import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelService;
import org.DigiTwinStudio.DigiTwin_Backend.services.MarketPlaceService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for accessing and searching the AAS model marketplace.
 */
@RestController
@RequestMapping("/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {
    private final MarketPlaceService  marketPlaceService;
    private final AASModelService aasModelService;

    /**
     * Lists all published marketplace entries.
     *
     * @return all marketplace entries as DTOs
     */
    @GetMapping()
    public ResponseEntity<List<MarketplaceEntryDto>> listAllEntries() {
        return ResponseEntity.ok(marketPlaceService.listAllEntries());
    }

    /**
     * Gets the published AAS model for a marketplace entry.
     *
     * @param entryId the marketplace entry ID
     * @return the published AAS model as DTO
     */
    @GetMapping("/{entryId}")
    public ResponseEntity<AASModelDto> getModelByEntryId(@PathVariable String entryId) {
        return ResponseEntity.ok(marketPlaceService.getPublishedModel(entryId));
    }

    /**
     * Copies a published marketplace entry model into the current user's workspace.
     *
     * @param entryId the entry ID to copy
     * @param jwt     the authenticated user's JWT
     * @return HTTP 200 OK if successful
     */
    @PostMapping("/{entryId}/add-to-user")
    public ResponseEntity<Void> addEntryToUser(@PathVariable String entryId, @AuthenticationPrincipal Jwt jwt) {
        aasModelService.addEntryModelToUser(entryId, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    /**
     * Returns all tags in the system.
     *
     * @return list of all tags
     */
    @GetMapping("/tags")
    public ResponseEntity<List<Tag>> getAllTags() {
        List<Tag> tags = marketPlaceService.getAllTags();
        return ResponseEntity.ok(tags);
    }

    /**
     * Searches marketplace entries using optional text, date, and tag filters.
     *
     * @param request search parameters
     * @return list of matching entries as DTOs
     */
    @PostMapping("/search")
    public ResponseEntity<List<MarketplaceEntryDto>> search(@RequestBody MarketplaceSearchRequest request) {
        return ResponseEntity.ok(marketPlaceService.search(request));
    }
}
