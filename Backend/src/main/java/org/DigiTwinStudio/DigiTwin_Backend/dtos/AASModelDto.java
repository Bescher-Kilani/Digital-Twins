package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.*;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AASModelDto {

    private String id;

    private AssetAdministrationShell aas;

    private PublishMetadataDto publishMetadata;

    private boolean published;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
