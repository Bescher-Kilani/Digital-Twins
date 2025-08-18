package org.DigiTwinStudio.DigiTwin_Backend.adapter;

import com.fasterxml.jackson.databind.JsonNode;

import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ExportException;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.aasx.InMemoryFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.junit.jupiter.MockitoExtension;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AAS4jAdapterTest {

    private AAS4jAdapter adapter;

    private final String testJsonString = "{\"test\":\"value\"}";
    private final String testIdShort = "TestAAS";

    @BeforeEach
    void setup() {
        adapter = new AAS4jAdapter();
    }

    // --- serializeToJsonString() Tests ---
    @Test
    void serializeToJsonString_shouldHandleComplexObjects() throws SerializationException {
        // --- Arrange ---
        DefaultAssetAdministrationShell testAAS = new DefaultAssetAdministrationShell.Builder()
                .idShort("ComplexAAS")
                .build();

        DefaultEnvironment environment = new DefaultEnvironment();
        environment.setAssetAdministrationShells(List.of(testAAS));

        // --- Act ---
        String result = adapter.serializeToJsonString(environment);

        // --- Assert ---
        assertNotNull(result);
        assertTrue(result.contains("ComplexAAS"));
        assertTrue(result.contains("assetAdministrationShells"));
    }

    @Test
    void serializeToJsonString_shouldReturnJsonString_whenSerializationSucceeds() throws SerializationException {
        // --- Arrange ---
        DefaultAssetAdministrationShell testAAS = new DefaultAssetAdministrationShell.Builder()
                .idShort("TestAAS")
                .build();

        // --- Act ---
        String result = adapter.serializeToJsonString(testAAS);

        // --- Assert ---
        assertNotNull(result);
        assertTrue(result.contains("TestAAS"));
    }

    @Test
    void serializeToJsonString_shouldThrowSerializationException_whenObjectIsNull() {
        // --- Act & Assert ---
        assertThrows(SerializationException.class, () -> adapter.serializeToJsonString(null));
    }

    // --- createEmptyAAS() Tests ---
    @Test
    void createEmptyAAS_shouldCreateAASWithGivenIdShort() {
        // --- Arrange ---
        // No setup needed

        // --- Act ---
        DefaultAssetAdministrationShell result = adapter.createEmptyAAS(testIdShort);

        // --- Assert ---
        assertNotNull(result);
        assertEquals(testIdShort, result.getIdShort());
    }

    @Test
    void createEmptyAAS_shouldCreateAASWithNullIdShort() {
        // --- Act ---
        DefaultAssetAdministrationShell result = adapter.createEmptyAAS(null);

        // --- Assert ---
        assertNotNull(result);
        assertNull(result.getIdShort());
    }

    @Test
    void createEmptyAAS_shouldCreateAASWithEmptyIdShort() {
        // --- Arrange ---
        String emptyIdShort = "";

        // --- Act ---
        DefaultAssetAdministrationShell result = adapter.createEmptyAAS(emptyIdShort);

        // --- Assert ---
        assertNotNull(result);
        assertEquals("", result.getIdShort());
    }

    @Test
    void createEmptyAAS_shouldHandleSpecialCharactersInIdShort() {
        // --- Arrange ---
        String specialIdShort = "AAS_Test-123_ÄÖÜ";

        // --- Act ---
        DefaultAssetAdministrationShell result = adapter.createEmptyAAS(specialIdShort);

        // --- Assert ---
        assertNotNull(result);
        assertEquals(specialIdShort, result.getIdShort());
    }

    // --- aasModelToDefaultEnvironment() Tests ---
    @Test
    void aasModelToDefaultEnvironment_shouldThrowException_whenModelIsNull() {

        // --- Act & Assert ---
        assertThrows(NullPointerException.class, () -> adapter.aasModelToDefaultEnvironment(null));
    }

    @Test
    void aasModelToDefaultEnvironment_shouldCreateEnvironmentWithAAS() {
        // --- Arrange ---
        AASModel mockModel = mock(AASModel.class);
        DefaultAssetAdministrationShell mockAAS = mock(DefaultAssetAdministrationShell.class);
        when(mockModel.getAas()).thenReturn(mockAAS);
        when(mockModel.getSubmodels()).thenReturn(null);

        // --- Act ---
        DefaultEnvironment result = adapter.aasModelToDefaultEnvironment(mockModel);

        // --- Assert ---
        assertNotNull(result);
        assertNotNull(result.getAssetAdministrationShells());
        assertEquals(1, result.getAssetAdministrationShells().size());
        assertEquals(mockAAS, result.getAssetAdministrationShells().getFirst());
        verify(mockModel).getAas();
        verify(mockModel).getSubmodels();
    }

    @Test
    void aasModelToDefaultEnvironment_shouldCreateEnvironmentWithAASAndSubmodels() {
        // --- Arrange ---
        AASModel mockModel = mock(AASModel.class);
        DefaultAssetAdministrationShell mockAAS = mock(DefaultAssetAdministrationShell.class);
        DefaultSubmodel mockSubmodel1 = mock(DefaultSubmodel.class);
        DefaultSubmodel mockSubmodel2 = mock(DefaultSubmodel.class);
        List<DefaultSubmodel> submodels = List.of(mockSubmodel1, mockSubmodel2);

        when(mockModel.getAas()).thenReturn(mockAAS);
        when(mockModel.getSubmodels()).thenReturn(submodels);

        // --- Act ---
        DefaultEnvironment result = adapter.aasModelToDefaultEnvironment(mockModel);

        // --- Assert ---
        assertNotNull(result);
        assertNotNull(result.getAssetAdministrationShells());
        assertEquals(1, result.getAssetAdministrationShells().size());
        assertEquals(mockAAS, result.getAssetAdministrationShells().getFirst());

        assertNotNull(result.getSubmodels());
        assertEquals(2, result.getSubmodels().size());
        assertTrue(result.getSubmodels().contains(mockSubmodel1));
        assertTrue(result.getSubmodels().contains(mockSubmodel2));

        verify(mockModel).getAas();
        verify(mockModel).getSubmodels();
    }

    @Test
    void aasModelToDefaultEnvironment_shouldCreateEnvironmentWithEmptySubmodelsList() {
        // --- Arrange ---
        AASModel mockModel = mock(AASModel.class);
        DefaultAssetAdministrationShell mockAAS = mock(DefaultAssetAdministrationShell.class);
        when(mockModel.getAas()).thenReturn(mockAAS);
        when(mockModel.getSubmodels()).thenReturn(new ArrayList<>());

        // --- Act ---
        DefaultEnvironment result = adapter.aasModelToDefaultEnvironment(mockModel);

        // --- Assert ---
        assertNotNull(result);
        assertNotNull(result.getAssetAdministrationShells());
        assertEquals(1, result.getAssetAdministrationShells().size());
        assertEquals(mockAAS, result.getAssetAdministrationShells().getFirst());

        // Submodels should be empty and not null
        assertNotNull(result.getSubmodels());
        assertTrue(result.getSubmodels().isEmpty());

        verify(mockModel).getAas();
        verify(mockModel).getSubmodels();
    }

    // --- parseSubmodelFromJson() Tests ---
    @Test
    void parseSubmodelFromJson_shouldReturnSubmodel_whenJsonIsValid() {
        // --- Arrange ---
        DefaultSubmodel originalSubmodel = new DefaultSubmodel.Builder()
                .idShort("TestSubmodel")
                .build();

        // First serialize to get valid JSON
        JsonNode validJson = adapter.serializeToJson(originalSubmodel);

        // --- Act ---
        DefaultSubmodel result = adapter.parseSubmodelFromJson(validJson);

        // --- Assert ---
        assertNotNull(result);
        assertEquals("TestSubmodel", result.getIdShort());
    }

    @Test
    void parseSubmodelFromJson_shouldThrowRuntimeException_whenJsonIsInvalid() {
        // --- Arrange ---
        JsonNode invalidJsonNode = mock(JsonNode.class);

        // --- Act & Assert ---
        assertThrows(RuntimeException.class, () -> adapter.parseSubmodelFromJson(invalidJsonNode));
    }

    // --- serializeToJson() Tests ---
    @Test
    void serializeToJson_shouldReturnJsonNode_whenObjectIsValid() {
        // --- Arrange ---
        DefaultAssetAdministrationShell testAAS = new DefaultAssetAdministrationShell.Builder()
                .idShort("TestAAS")
                .build();

        // --- Act ---
        JsonNode result = adapter.serializeToJson(testAAS);

        // --- Assert ---
        assertNotNull(result);
    }

    @Test
    void serializeToJson_shouldThrowExportException_whenObjectIsNull() {
        // --- Act & Assert ---
        assertThrows(ExportException.class, () -> adapter.serializeToJson(null));
    }

    // --- serializeToAASX() Tests ---
    @Test
    void serializeToAASX_shouldCompleteSuccessfully_whenParametersAreValid() {
        // --- Arrange ---
        Environment mockEnvironment = mock(Environment.class);
        Collection<InMemoryFile> emptyFiles = new ArrayList<>();
        OutputStream mockOutputStream = mock(OutputStream.class);

        // --- Act & Assert ---
        // This test just verifies the method can be called without throwing an exception
        // The actual implementation will depend on the AASXSerializer behavior
        assertDoesNotThrow(() -> adapter.serializeToAASX(mockEnvironment, emptyFiles, mockOutputStream));
    }
}