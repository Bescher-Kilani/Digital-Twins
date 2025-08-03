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



@RestController
@RequestMapping
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    /**
     * Exports a stored AAS model by its ID in the specified format (e.g. JSON or AASX),
     * and returns it as a downloadable file in the HTTP response.
     * <p>
     * This endpoint is typically used to trigger a download in the user's browser.
     * The response contains appropriate HTTP headers to:
     * <ul>
     *   <li>Set the correct <strong>Content-Type</strong> (e.g., {@code application/json}, {@code application/asset-administration-shell-package})</li>
     *   <li>Set the <strong>Content-Disposition</strong> header to {@code attachment}, so the browser opens the download dialog</li>
     *   <li>Provide a meaningful <strong>filename</strong> like {@code model-xyz.json} or {@code model-abc.aasx}</li>
     * </ul>
     *
     * @param id      the ID of the stored AAS model
     * @param name    the desired filename (without extension) for the exported file
     * @param format  the export format (e.g., JSON or AASX)
     * @return a {@link ResponseEntity} containing the model as a byte stream, download headers, and content type
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
