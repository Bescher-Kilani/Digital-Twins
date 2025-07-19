package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.*;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;


import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AASModelDto {

    private String id;

    private DefaultAssetAdministrationShell aas;

    private List<DefaultSubmodel> submodels;

    private PublishMetadataDto publishMetadata;

    private boolean published;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
