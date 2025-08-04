package org.DigiTwinStudio.DigiTwin_Backend.controller;

import lombok.RequiredArgsConstructor;

import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportFormat;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportedFile;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ExportException;
import org.DigiTwinStudio.DigiTwin_Backend.services.ExportService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for exporting AAS models as downloadable files.
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    /**
     * Exports a stored AAS model as a downloadable file in the specified format.
     *
     * @param id      the ID of the stored AAS model
     * @param name    the desired filename (without extension)
     * @param format  the export format (e.g., JSON or AASX)
     * @param jwt     the authentication token (to extract user id)
     * @return HTTP response with download headers and model bytes
     * @throws ExportException if the model cannot be exported
     */
    @GetMapping("/models/{id}/{name}/export/{format}")
    public ResponseEntity<byte[]> exportModel(
            @PathVariable String id,
            @PathVariable String name,
            @PathVariable ExportFormat format,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();

        ExportedFile exported = exportService.export(id, name, format, userId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;" +
                        " filename=\"" + exported.filename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, exported.contentType())
                .body(exported.bytes());
    }
}
