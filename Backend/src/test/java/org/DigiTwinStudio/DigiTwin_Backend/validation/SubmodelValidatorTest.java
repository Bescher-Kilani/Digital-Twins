package org.DigiTwinStudio.DigiTwin_Backend.validation;

import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;

import de.fraunhofer.iosb.ilt.faaast.service.model.validation.ModelValidator;

import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultFile;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelElementCollection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubmodelValidatorTest {

    @InjectMocks
    private SubmodelValidator validator;

    // testing valid submodel
    @Test
    void validate_shouldPass_whenAllElementsValid() {
        DefaultFile file = new DefaultFile();
        file.setIdShort("specFile");
        file.setContentType("application/pdf"); // allowed type
        file.setValue("file-id-123");

        DefaultProperty prop = new DefaultProperty();
        prop.setIdShort("someProp");

        DefaultSubmodel submodel = new DefaultSubmodel();
        submodel.setSubmodelElements(List.of(file, prop));

        try (MockedStatic<ModelValidator> mv = mockStatic(ModelValidator.class)) {
            assertDoesNotThrow(() -> validator.validate(submodel));
            mv.verify(() -> ModelValidator.validate(submodel), times(1));
        }
    }

    @Test
    void validate_shouldPass_whenNoElements() {
        DefaultSubmodel submodel = new DefaultSubmodel();
        submodel.setSubmodelElements(null); // no elements present

        try (MockedStatic<ModelValidator> mv = mockStatic(ModelValidator.class)) {
            assertDoesNotThrow(() -> validator.validate(submodel));
            mv.verify(() -> ModelValidator.validate(submodel), times(1));
        }
    }

    @Test
    void validate_shouldPass_whenEmptyElementsList() {
        DefaultSubmodel submodel = new DefaultSubmodel();
        submodel.setSubmodelElements(new ArrayList<>());

        try (MockedStatic<ModelValidator> mv = mockStatic(ModelValidator.class)) {
            assertDoesNotThrow(() -> validator.validate(submodel));
            mv.verify(() -> ModelValidator.validate(submodel), times(1));
        }
    }

    @Test
    void validate_shouldPass_whenCollectionEmpty() {
        DefaultSubmodelElementCollection collection = new DefaultSubmodelElementCollection();
        collection.setIdShort("container");
        collection.setValue(new ArrayList<>()); // empty children

        DefaultSubmodel submodel = new DefaultSubmodel();
        submodel.setSubmodelElements(List.of(collection));

        try (MockedStatic<ModelValidator> mv = mockStatic(ModelValidator.class)) {
            assertDoesNotThrow(() -> validator.validate(submodel));
            mv.verify(() -> ModelValidator.validate(submodel), times(1));
        }
    }

    @Test
    void validate_shouldPass_whenFileHasAllowedMimeTypes() {
        String[] allowed = {
                "application/json",
                "application/aasx+zip",
                "application/pdf",
                "image/png"
        };

        for (String mime : allowed) {
            DefaultFile file = new DefaultFile();
            file.setIdShort("file-" + mime);
            file.setContentType(mime);
            file.setValue("file-id");

            DefaultSubmodel submodel = new DefaultSubmodel();
            submodel.setSubmodelElements(List.of(file));

            try (MockedStatic<ModelValidator> mv = mockStatic(ModelValidator.class)) {
                assertDoesNotThrow(() -> validator.validate(submodel));
                mv.verify(() -> ModelValidator.validate(submodel), times(1));
            }
        }
    }

    @Test
    void validate_shouldPass_whenMultipleElementsAllValid() {
        DefaultProperty p1 = new DefaultProperty(); p1.setIdShort("p1");
        DefaultProperty p2 = new DefaultProperty(); p2.setIdShort("p2");

        DefaultFile f1 = new DefaultFile();
        f1.setIdShort("f1");
        f1.setContentType("application/json");
        f1.setValue("id-1");

        DefaultFile f2 = new DefaultFile();
        f2.setIdShort("f2");
        f2.setContentType("image/png");
        f2.setValue("id-2");

        DefaultSubmodel submodel = new DefaultSubmodel();
        submodel.setSubmodelElements(List.of(p1, f1, p2, f2));

        try (MockedStatic<ModelValidator> mv = mockStatic(ModelValidator.class)) {
            assertDoesNotThrow(() -> validator.validate(submodel));
            mv.verify(() -> ModelValidator.validate(submodel), times(1));
        }
    }

    // testing idShort validation
    @Test
    void validate_shouldThrow_whenIdShortIsBlank() {
        DefaultProperty bad = new DefaultProperty();
        bad.setIdShort("   ");

        DefaultSubmodel submodel = new DefaultSubmodel();
        submodel.setSubmodelElements(List.of(bad));

        try (MockedStatic<ModelValidator> mv = mockStatic(ModelValidator.class)) {
            assertThrows(BadRequestException.class, () -> validator.validate(submodel));
            mv.verify(() -> ModelValidator.validate(submodel), times(1));
        }
    }

    @Test
    void validate_shouldThrow_whenIdShortIsNull() {
        DefaultProperty bad = new DefaultProperty();
        bad.setIdShort(null);

        DefaultSubmodel submodel = new DefaultSubmodel();
        submodel.setSubmodelElements(List.of(bad));

        try (MockedStatic<ModelValidator> mv = mockStatic(ModelValidator.class)) {
            assertThrows(BadRequestException.class, () -> validator.validate(submodel));
            mv.verify(() -> ModelValidator.validate(submodel), times(1));
        }
    }

    @Test
    void validate_shouldThrow_whenDeeplyNestedInvalidChild() {
        DefaultProperty leafBad = new DefaultProperty();
        leafBad.setIdShort(" "); // invalid

        DefaultSubmodelElementCollection level2 = new DefaultSubmodelElementCollection();
        level2.setIdShort("level2");
        level2.setValue(List.of(leafBad));

        DefaultSubmodelElementCollection level1 = new DefaultSubmodelElementCollection();
        level1.setIdShort("level1");
        level1.setValue(List.of(level2));

        DefaultSubmodel submodel = new DefaultSubmodel();
        submodel.setSubmodelElements(List.of(level1));

        try (MockedStatic<ModelValidator> mv = mockStatic(ModelValidator.class)) {
            assertThrows(BadRequestException.class, () -> validator.validate(submodel));
            mv.verify(() -> ModelValidator.validate(submodel), times(1));
        }
    }

    // testing MIME type validation
    @Test
    void validate_shouldThrow_whenFileHasUnsupportedMimeType() {
        DefaultFile file = new DefaultFile();
        file.setIdShort("rawText");
        file.setContentType("text/plain"); // not allowed
        file.setValue("file-id-xyz");

        DefaultSubmodel submodel = new DefaultSubmodel();
        submodel.setSubmodelElements(List.of(file));

        try (MockedStatic<ModelValidator> mv = mockStatic(ModelValidator.class)) {
            BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validate(submodel));
            assertTrue(ex.getMessage().contains("Unsupported MIME type"));
            mv.verify(() -> ModelValidator.validate(submodel), times(1));
        }
    }

    @Test
    void validate_shouldThrow_whenFileHasNullMimeType() {
        DefaultFile file = new DefaultFile();
        file.setIdShort("noMime");
        file.setContentType(null); // null -> not in allowed set
        file.setValue("id");

        DefaultSubmodel submodel = new DefaultSubmodel();
        submodel.setSubmodelElements(List.of(file));

        try (MockedStatic<ModelValidator> mv = mockStatic(ModelValidator.class)) {
            assertThrows(BadRequestException.class, () -> validator.validate(submodel));
            mv.verify(() -> ModelValidator.validate(submodel), times(1));
        }
    }

    @Test
    void validate_shouldThrow_whenFirstOfManyIsInvalid() {
        DefaultProperty bad = new DefaultProperty();
        bad.setIdShort(""); // invalid

        DefaultProperty good = new DefaultProperty();
        good.setIdShort("ok");

        DefaultSubmodel submodel = new DefaultSubmodel();
        submodel.setSubmodelElements(List.of(bad, good));

        try (MockedStatic<ModelValidator> mv = mockStatic(ModelValidator.class)) {
            assertThrows(BadRequestException.class, () -> validator.validate(submodel));
            mv.verify(() -> ModelValidator.validate(submodel), times(1));
        }
    }

    // testing recursive validation in collections
    @Test
    void validate_shouldThrow_whenChildElementInCollectionIsInvalid() {
        DefaultProperty child = new DefaultProperty();
        child.setIdShort(""); // invalid

        DefaultSubmodelElementCollection collection = new DefaultSubmodelElementCollection();
        collection.setIdShort("container");
        collection.setValue(List.of(child));

        DefaultSubmodel submodel = new DefaultSubmodel();
        submodel.setSubmodelElements(List.of(collection));

        try (MockedStatic<ModelValidator> mv = mockStatic(ModelValidator.class)) {
            assertThrows(BadRequestException.class, () -> validator.validate(submodel));
            mv.verify(() -> ModelValidator.validate(submodel), times(1));
        }
    }

    // testing FAAAST integration
    @Test
    void validate_shouldWrapFaaastValidationException() {
        DefaultSubmodel submodel = new DefaultSubmodel();

        try (MockedStatic<ModelValidator> mv = mockStatic(ModelValidator.class)) {
            mv.when(() -> ModelValidator.validate(submodel))
                    .thenThrow(new de.fraunhofer.iosb.ilt.faaast.service.model.exception.ValidationException("boom"));

            BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validate(submodel));
            assertTrue(ex.getMessage().startsWith("Not Valid submodel"));
            mv.verify(() -> ModelValidator.validate(submodel), times(1));
        }
    }
}
