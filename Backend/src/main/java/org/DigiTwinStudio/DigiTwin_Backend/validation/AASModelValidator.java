package org.DigiTwinStudio.DigiTwin_Backend.validation;

import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;

import lombok.RequiredArgsConstructor;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;

import de.fraunhofer.iosb.ilt.faaast.service.model.validation.ModelValidator;

import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.PublishMetadata;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Component responsible for validating complete AASModel instances.
 * - Global AAS structure via FAAAST ModelValidator
 * - Per-submodel validation via SubmodelValidator
 * - Project-specific metadata validation
 */
@Component
@RequiredArgsConstructor
public class AASModelValidator {

    private final SubmodelValidator submodelValidator;

    /**
     * Validate a full AASModel.
     * <p>
     * 1. Validate AAS (AssetAdministrationShell) globally via FAAST.
     * 2. Validate all attached Submodels with SubmodelValidator.
     * 3. If model is marked as published, ensure PublishMetadata is complete.
     *
     * @param model the AASModel to validate
     */
    public void validate(AASModel model) {
        // 1. Validate AAS structure
        DefaultAssetAdministrationShell aas = model.getAas();
        if (aas == null) {
            throw new BadRequestException("AASModel must contain an AssetAdministrationShell");
        }

        try {
            ModelValidator.validate(aas);
        } catch (de.fraunhofer.iosb.ilt.faaast.service.model.exception.ValidationException e) {
            throw new BadRequestException("AAS validation failed: " + e.getMessage(), e);
        }

        // 2. Validate all submodels in env
        List<DefaultSubmodel> submodels = model.getSubmodels();

        for (DefaultSubmodel sm : submodels) {
            submodelValidator.validate(sm);
        }

        // 3. Validate PublishMetadata
        if (model.isPublished()) {
            validatePublishMetadata(model.getPublishMetadata());
        }
    }

    private void validatePublishMetadata(PublishMetadata metadata) {
        if (metadata == null) {
            throw new BadRequestException("PublishMetadata must be provided when publishing a model");
        }
        if (metadata.getAuthor() == null || metadata.getAuthor().isBlank()) {
            throw new BadRequestException("PublishMetadata.author must not be null or blank");
        }
        if (metadata.getShortDescription() == null || metadata.getShortDescription().isBlank()) {
            throw new BadRequestException("PublishMetadata.shortDescription must not be null or blank");
        }
        if (metadata.getTagIds() == null || metadata.getTagIds().isEmpty()) {
            throw new BadRequestException("PublishMetadata.tagIds must not be null or empty");
        }
    }
}
