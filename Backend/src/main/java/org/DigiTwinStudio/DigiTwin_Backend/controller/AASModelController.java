package org.DigiTwinStudio.DigiTwin_Backend.controller;

import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AASModelController {

    private final AASModelService aasModelService;

    @PostMapping("/create-empty")
    public AASModelDto createEmptyModel(@RequestParam String userId) {
        return aasModelService.createEmpty(userId);
    }
}
