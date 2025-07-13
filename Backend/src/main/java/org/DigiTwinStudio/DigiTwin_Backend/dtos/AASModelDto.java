package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.*;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;


import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AASModelDto {

    private String id;

    private AssetAdministrationShell aas;

    private List<Submodel> submodels;

    private PublishMetadataDto publishMetadata;

    private boolean published;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
