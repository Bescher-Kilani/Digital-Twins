package org.DigiTwinStudio.DigiTwin_Backend.controller;

import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportFormat;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportedFile;
import org.DigiTwinStudio.DigiTwin_Backend.services.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    /**
     * Exports a stored AAS model in the specified format.
     *
     * @param modelId the ID of the model to export
     * @param format the desired export format (e.g., {@code AASX}, {@code JSON}
     * @param jwt the JWT token of the authenticated user
     * @return a ResponseEntity containing the exported file as a byte array
     */
    @GetMapping("/models/{id}/{name}/export/{format}")
    public ResponseEntity<byte[]> exportModel(
            @PathVariable String id,
            @PathVariable String name,
            @PathVariable ExportFormat format) {

        ExportedFile exported = exportService.export(id, name, format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;" +
                        " filename=\"" + exported.filename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, exported.contentType())
                .body(exported.bytes());
    }

}
