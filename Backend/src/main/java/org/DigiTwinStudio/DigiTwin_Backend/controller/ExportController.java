package org.DigiTwinStudio.DigiTwin_Backend.controller;

import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportFormat;
import org.DigiTwinStudio.DigiTwin_Backend.services.ExportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/models/{modelId}/export/{format}")
    public ResponseEntity<byte[]> exportStoredModel(@PathVariable String modelId, @PathVariable String format, @AuthenticationPrincipal Jwt jwt) {
        // ToDo: Add Guest-logic
        String userId = jwt.getSubject();
        return ResponseEntity.ok(this.exportService.exportStoredModel(modelId, ExportFormat.valueOf(format)));
    }

}
