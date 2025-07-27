package org.DigiTwinStudio.DigiTwin_Backend.controller;

import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceEntryDto;
import org.DigiTwinStudio.DigiTwin_Backend.services.MarketPlaceService;
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
     * Filters marketplace entries by one or more tags.
     *
     * <p>Example request:
     * <pre>
     * GET /marketplace?tag=java&tag=spring&tag=backend
     * </pre>
     *
     * @param tags a list of tags to filter the entries by
     * @return a list of matching {@link MarketplaceEntryDto} objects
     */
    @GetMapping(params = "tag")
    public ResponseEntity<List<MarketplaceEntryDto>> filterByTag(@RequestParam List<String> tags) {
        return ResponseEntity.ok(marketPlaceService.searchByTags(tags));
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
        marketPlaceService.addEntryModelToUser(entryId, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

}
