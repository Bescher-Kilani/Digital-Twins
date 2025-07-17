package org.DigiTwinStudio.DigiTwin_Backend.controller;

import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AASModelController {

    private final AASModelService aasModelService;

    @GetMapping
    public ResponseEntity<List<AASModelDto>> listAASModels(@RequestHeader("userId") String userId) {
        return ResponseEntity.ok(aasModelService.getAllForUser(userId));
    }

}
