package org.DigiTwinStudio.DigiTwin_Backend.domain;

import lombok.*;

import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents an Asset Administration Shell (AAS) model entity stored in the database.
 */
@Document("AASModels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AASModel {

    @Id
    private String id;

    private String ownerId;

    private DefaultAssetAdministrationShell aas;

    private List<DefaultSubmodel> submodels;

    private PublishMetadata publishMetadata;

    private boolean published;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
