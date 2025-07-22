package org.DigiTwinStudio.DigiTwin_Backend.controller;

import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportFormat;
import org.DigiTwinStudio.DigiTwin_Backend.services.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping
    public ResponseEntity<byte[]> exportStoredModel(@PathVariable String id, @RequestParam ExportFormat format) {
        byte[] data = exportService.exportStoredModel(id, format);
        return buildExportResponse(data, format, "model-" + id);
    }

    private ResponseEntity<byte[]> buildExportResponse(byte[] content, ExportFormat format, String filenamePrefix) {
        String contentType = format == ExportFormat.JSON ? MediaType.APPLICATION_JSON_VALUE : "application/aasx+zip";
        String extension = format.name().toLowerCase();
        String filename = filenamePrefix + "." + extension;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(content);
    }
}
