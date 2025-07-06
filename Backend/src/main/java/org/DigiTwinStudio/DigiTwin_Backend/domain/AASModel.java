package org.DigiTwinStudio.DigiTwin_Backend.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;

import java.time.LocalDateTime;

@Document("AASModels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AASModel {

    @Id
    private String id;

    private String ownerId;

    private AssetAdministrationShell aas;

    private PublishMetadata publishMetadata;

    private boolean deleted;

    private boolean published;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}