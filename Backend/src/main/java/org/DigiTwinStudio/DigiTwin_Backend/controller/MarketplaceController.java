package org.DigiTwinStudio.DigiTwin_Backend.controller;

import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportFormat;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportedFile;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceEntryDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceSearchRequest;
import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelService;
import org.DigiTwinStudio.DigiTwin_Backend.services.MarketPlaceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {
    private final MarketPlaceService  marketPlaceService;
    private final AASModelService aasModelService;

    /**
     * Retrieves a list of all published marketplace entries.
     *
     * <p>Example request:
     * <pre>
     * GET /marketplace
     * </pre>
     *
     * @return a {@link ResponseEntity} containing a list of {@link MarketplaceEntryDto} objects
     */
    @GetMapping()
    public ResponseEntity<List<MarketplaceEntryDto>> listAllEntries() {
        return ResponseEntity.ok(marketPlaceService.listAllEntries());
    }

    /**
     * Retrieves the published AAS model associated with the given marketplace entry ID.
     *
     * <p>Example request:
     * <pre>
     * GET /marketplace/{entryID}
     * </pre>
     *
     * @param entryId the ID of the marketplace entry whose AAS model should be retrieved
     * @return a {@link ResponseEntity} containing the corresponding {@link AASModelDto}
     */
    @GetMapping("/{entryId}")
    public ResponseEntity<AASModelDto> getModelByEntryId(@PathVariable String entryId) {
        return ResponseEntity.ok(marketPlaceService.getPublishedModel(entryId));
    }

    /**
     * Adds the AAS model of a marketplace entry to the authenticated user's workspace.
     *
     * <p>This endpoint:
     * <ul>
     *     <li>Extracts the user ID from the JWT token.</li>
     *     <li>Creates a new empty AAS model for the user.</li>
     *     <li>Copies the published model from the given entry into the user's model.</li>
     * </ul>
     *
     * <p>Example request:
     * <pre>
     * POST /marketplace/{entryId}/add-to-user
     * </pre>
     *
     * @param entryId the ID of the marketplace entry to be copied
     * @param jwt the JWT containing user identity
     * @return HTTP 200 OK if successful
     **/
    @PostMapping("/{entryId}/add-to-user")
    public ResponseEntity<Void> addEntryToUser(@PathVariable String entryId, @AuthenticationPrincipal Jwt jwt) {
        aasModelService.addEntryModelToUser(entryId, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    /**
     * Returns all tags available in the system.
     *
     * <p>Example request:
     * <pre>
     * GET /tags
     * </pre>
     *
     * @return a list of all {@link Tag} objects
     */
    @GetMapping("/tags")
    public ResponseEntity<List<Tag>> getAllTags() {
        List<Tag> tags = marketPlaceService.getAllTags();
        return ResponseEntity.ok(tags);
    }

    /**
     * Endpoint to search marketplace entries.
     * <p>
     * Accepts optional full-text, date, and tag filters via the request body and returns matching entries.
     * </p>
     *
     * Examples:
     * <ul>
     *   <li>Text only: {"searchText":"spring"}</li>
     *   <li>Date only: {"publishedAfter":"2024-01-01T00:00:00"}</li>
     *   <li>Tags only: {"tagIds":["java","backend"]}</li>
     *   <li>Combined: {"searchText":"spring","publishedAfter":"2024-01-01T00:00:00","tagIds":["java"]}</li>
     * </ul>
     *
     * @param request search parameters
     * @return 200 OK with a list of matching entries as DTOs
     */
    @PostMapping("/search")
    public ResponseEntity<List<MarketplaceEntryDto>> search(@RequestBody MarketplaceSearchRequest request) {
        return ResponseEntity.ok(marketPlaceService.search(request));
    }

}
