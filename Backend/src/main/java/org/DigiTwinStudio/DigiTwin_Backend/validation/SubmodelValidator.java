package org.DigiTwinStudio.DigiTwin_Backend.validation;

import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;

import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.File;

import de.fraunhofer.iosb.ilt.faaast.service.model.validation.ModelValidator;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Component responsible for validating Submodel instances
 * both against the AAS metamodel (via FAAAST) and project-specific rules.
 */
@Component
public class SubmodelValidator {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/json",
            "application/aasx+zip",
            "application/pdf",
            "image/png"
    );

    /**
     * Validates a submodel using FAAAST and custom element checks.
     *
     * @param submodel the submodel to validate
     * @throws BadRequestException if validation fails
     */
    public void validate(DefaultSubmodel submodel) {

        try{
            // 1. Global metamodel validation using FAAAST
            ModelValidator.validate(submodel);

            // 2. Project-specific checks for all SubmodelElements
            if (submodel.getSubmodelElements() != null) {
                for (SubmodelElement element : submodel.getSubmodelElements()) {
                    validateElement(element);
                }
            }
        } catch (de.fraunhofer.iosb.ilt.faaast.service.model.exception.ValidationException e){
            throw new BadRequestException("Not Valid submodel" + e.getMessage(), e);
        }

    }

    private void validateElement(SubmodelElement element) {
        // idShort must be set and not empty
        if (element.getIdShort() == null || element.getIdShort().isBlank()) {
            throw new BadRequestException("Element idShort must not be null or empty");
        }
        // For File elements, enforce allowed MIME types
        if (element instanceof File) {
            String mime = ((File) element).getContentType();
            if (mime == null || mime.isBlank() || !ALLOWED_MIME_TYPES.contains(mime)) {
                throw new BadRequestException("Unsupported MIME type: " + mime);
            }
        }
        // For collections, validate each member recursively
        if (element instanceof SubmodelElementCollection) {
            for (SubmodelElement child : ((SubmodelElementCollection) element).getValue()) {
                validateElement(child);
            }
        }

    }
}
