package org.DigiTwinStudio.DigiTwin_Backend.domain;

import lombok.*;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;

import java.time.LocalDateTime;
import java.util.List;

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

    private List<Submodel> submodels;

    private PublishMetadata publishMetadata;

    private boolean deleted;

    private boolean published;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}