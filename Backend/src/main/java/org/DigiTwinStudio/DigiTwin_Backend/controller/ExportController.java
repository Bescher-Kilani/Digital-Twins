package org.DigiTwinStudio.DigiTwin_Backend.controller;

import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportFormat;
import org.DigiTwinStudio.DigiTwin_Backend.services.ExportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/models/{ModelId}/export/{format}")
    public ResponseEntity<byte[]> exportStoredModel(@PathVariable String ModelId, @PathVariable String format, Principal principal) {
        // ToDo: Add Guest-logic
        String userId = principal.getName();
        return ResponseEntity.ok(this.exportService.exportStoredModel(ModelId, ExportFormat.valueOf(format)));
    }

}
