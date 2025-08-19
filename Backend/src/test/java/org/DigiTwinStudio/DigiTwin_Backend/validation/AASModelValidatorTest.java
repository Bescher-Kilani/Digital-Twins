package org.DigiTwinStudio.DigiTwin_Backend.validation;

import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.PublishMetadata;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;

import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;

import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ValidationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import de.fraunhofer.iosb.ilt.faaast.service.model.validation.ModelValidator;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AASModelValidatorTest {

    @InjectMocks
    private AASModelValidator validator;

    @Mock
    private SubmodelValidator submodelValidator;

    private static AASModel createBaseModel(boolean published) {
        return AASModel.builder()
                .id("model-1")
                .ownerId("user-1")
                .published(published)
                .aas(new DefaultAssetAdministrationShell.Builder().idShort("TestAAS").build())
                .submodels(new ArrayList<>())
                .build();
    }

    private static PublishMetadata createValidPublishMetadata() {
        return PublishMetadata.builder()
                .author("Test Author")
                .shortDescription("Test Description")
                .tagIds(List.of("tag1", "tag2"))
                .build();
    }

    private static DefaultSubmodel createSubmodel(String id) {
        DefaultSubmodel submodel = new DefaultSubmodel();
        submodel.setId(id);
        submodel.setIdShort("TestSubmodel" + id);
        return submodel;
    }

    // testing validate() function for AAS structure
    @Test
    void validate_succeeds_withValidUnpublishedModel() {
        // Given: valid model with AAS but not published
        AASModel model = createBaseModel(false);

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .then(invocation -> null); // void method, no return

            // When & Then: no exception should be thrown
            assertDoesNotThrow(() -> validator.validate(model));

            // Verify ModelValidator was called
            modelValidatorMock.verify(() -> ModelValidator.validate(model.getAas()));
            verify(submodelValidator, never()).validate(any());
        }
    }

    @Test
    void validate_throwsBadRequest_whenAasIsNull() {
        AASModel model = createBaseModel(false);
        model.setAas(null);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(model));
        assertEquals("AASModel must contain an AssetAdministrationShell", ex.getMessage());
    }

    @Test
    void validate_wrapsModelValidatorException() {
        AASModel model = createBaseModel(false);

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .thenThrow(new ValidationException("Invalid AAS structure"));

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> validator.validate(model));
            assertTrue(ex.getMessage().contains("Not Valid submodel"));
            assertTrue(ex.getMessage().contains("Invalid AAS structure"));
            assertInstanceOf(ValidationException.class, ex.getCause());
        }
    }

    // testing validate() function for submodels
    @Test
    void validate_validatesAllSubmodels_whenPresent() {
        AASModel model = createBaseModel(false);
        DefaultSubmodel sub1 = createSubmodel("sub-1");
        DefaultSubmodel sub2 = createSubmodel("sub-2");
        model.setSubmodels(List.of(sub1, sub2));

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .then(invocation -> null);

            validator.validate(model);

            verify(submodelValidator).validate(sub1);
            verify(submodelValidator).validate(sub2);
        }
    }

    @Test
    void validate_skipsSubmodelValidation_whenSubmodelsNull() {
        AASModel model = createBaseModel(false);
        model.setSubmodels(null);

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .then(invocation -> null);

            validator.validate(model);

            verify(submodelValidator, never()).validate(any());
        }
    }

    @Test
    void validate_skipsSubmodelValidation_whenSubmodelsEmpty() {
        AASModel model = createBaseModel(false);
        model.setSubmodels(new ArrayList<>());

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .then(invocation -> null);

            // When
            validator.validate(model);

            verify(submodelValidator, never()).validate(any());
        }
    }

    @Test
    void validate_propagatesSubmodelValidationException() {
        AASModel model = createBaseModel(false);
        DefaultSubmodel submodel = createSubmodel("invalid-sub");
        model.setSubmodels(List.of(submodel));

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .then(invocation -> null);

            doThrow(new BadRequestException("Invalid submodel structure"))
                    .when(submodelValidator).validate(submodel);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> validator.validate(model));
            assertEquals("Invalid submodel structure", ex.getMessage());
        }
    }

    // testing validate() function for published models
    @Test
    void validate_validatesPublishMetadata_whenModelIsPublished() {
        AASModel model = createBaseModel(true);
        PublishMetadata metadata = createValidPublishMetadata();
        model.setPublishMetadata(metadata);

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .then(invocation -> null);

            assertDoesNotThrow(() -> validator.validate(model));
        }
    }

    @Test
    void validate_skipsPublishMetadataValidation_whenModelNotPublished() {
        AASModel model = createBaseModel(false);
        model.setPublishMetadata(null);

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .then(invocation -> null);

            assertDoesNotThrow(() -> validator.validate(model));
        }
    }

    @Test
    void validate_throwsBadRequest_whenPublishedModelHasNullMetadata() {
        AASModel model = createBaseModel(true);
        model.setPublishMetadata(null);

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .then(invocation -> null);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> validator.validate(model));
            assertEquals("PublishMetadata must be provided when publishing a model", ex.getMessage());
        }
    }

    // testing validatePublishMetadata functions
    @Test
    void validate_throwsBadRequest_whenAuthorIsNull() {
        AASModel model = createBaseModel(true);
        PublishMetadata metadata = createValidPublishMetadata();
        metadata.setAuthor(null);
        model.setPublishMetadata(metadata);

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .then(invocation -> null);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> validator.validate(model));
            assertEquals("PublishMetadata.author must not be null or blank", ex.getMessage());
        }
    }

    @Test
    void validate_throwsBadRequest_whenAuthorIsBlank() {
        AASModel model = createBaseModel(true);
        PublishMetadata metadata = createValidPublishMetadata();
        metadata.setAuthor("   ");
        model.setPublishMetadata(metadata);

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .then(invocation -> null);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> validator.validate(model));
            assertEquals("PublishMetadata.author must not be null or blank", ex.getMessage());
        }
    }

    @Test
    void validate_throwsBadRequest_whenShortDescriptionIsNull() {
        AASModel model = createBaseModel(true);
        PublishMetadata metadata = createValidPublishMetadata();
        metadata.setShortDescription(null);
        model.setPublishMetadata(metadata);

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .then(invocation -> null);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> validator.validate(model));
            assertEquals("PublishMetadata.shortDescription must not be null or blank", ex.getMessage());
        }
    }

    @Test
    void validate_throwsBadRequest_whenShortDescriptionIsBlank() {
        AASModel model = createBaseModel(true);
        PublishMetadata metadata = createValidPublishMetadata();
        metadata.setShortDescription("");
        model.setPublishMetadata(metadata);

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .then(invocation -> null);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> validator.validate(model));
            assertEquals("PublishMetadata.shortDescription must not be null or blank", ex.getMessage());
        }
    }

    @Test
    void validate_throwsBadRequest_whenTagIdsIsNull() {
        AASModel model = createBaseModel(true);
        PublishMetadata metadata = createValidPublishMetadata();
        metadata.setTagIds(null);
        model.setPublishMetadata(metadata);

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .then(invocation -> null);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> validator.validate(model));
            assertEquals("PublishMetadata.tagIds must not be null or empty", ex.getMessage());
        }
    }

    @Test
    void validate_throwsBadRequest_whenTagIdsIsEmpty() {
        AASModel model = createBaseModel(true);
        PublishMetadata metadata = createValidPublishMetadata();
        metadata.setTagIds(new ArrayList<>());
        model.setPublishMetadata(metadata);

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .then(invocation -> null);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> validator.validate(model));
            assertEquals("PublishMetadata.tagIds must not be null or empty", ex.getMessage());
        }
    }

    // testing comprehensive integration tests
    @Test
    void validate_validatesCompletePublishedModel() {
        // Given: complete published model with AAS, submodels, and metadata
        AASModel model = createBaseModel(true);
        DefaultSubmodel submodel = createSubmodel("sub-1");
        model.setSubmodels(List.of(submodel));
        model.setPublishMetadata(createValidPublishMetadata());

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .then(invocation -> null);

            validator.validate(model);

            // Then: all validations should be performed
            modelValidatorMock.verify(() -> ModelValidator.validate(model.getAas()));
            verify(submodelValidator).validate(submodel);
        }
    }

    @Test
    void validate_failsEarly_whenAasValidationFails() {
        // Given: model where AAS validation fails
        AASModel model = createBaseModel(true);
        DefaultSubmodel submodel = createSubmodel("sub-1");
        model.setSubmodels(List.of(submodel));
        model.setPublishMetadata(createValidPublishMetadata());

        try (MockedStatic<ModelValidator> modelValidatorMock = mockStatic(ModelValidator.class)) {
            modelValidatorMock.when(() -> ModelValidator.validate(any(DefaultAssetAdministrationShell.class)))
                    .thenThrow(new ValidationException("AAS validation failed"));

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> validator.validate(model));
            assertTrue(ex.getMessage().contains("Not Valid submodel"));

            // Submodel validation should not be called when AAS validation fails
            verify(submodelValidator, never()).validate(any());
        }
    }
}
