package org.DigiTwinStudio.DigiTwin_Backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.DigiTwinStudio.DigiTwin_Backend.domain.Template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SMTRepoClientTest {

    @Mock private RestClient restClient;
    @Mock private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private SMTRepoClient smtRepoClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ObjectNode mockResponse;
    private ArrayNode resultArray;

    @BeforeEach
    void setup() {
        // Setup RestClient mock chain
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        // Create mock JSON response structure
        mockResponse = objectMapper.createObjectNode();
        resultArray = objectMapper.createArrayNode();
        mockResponse.set("result", resultArray);
    }

    // --- Successful fetchTemplates() ---
    @Test
    void fetchTemplates_shouldReturnTemplatesFromValidResponse() {
        // --- Arrange ---
        ObjectNode templateItem = createValidTemplateItem("TestTemplate", "1", "0");
        resultArray.add(templateItem);

        when(responseSpec.body(JsonNode.class)).thenReturn(mockResponse);

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertEquals(1, result.size());
        Template template = result.get(0);
        assertEquals("TestTemplate", template.getName());
        assertEquals("1", template.getVersion());
        assertEquals("0", template.getRevision());
        assertEquals(2, template.getDescriptions().size());
        assertEquals("English description", template.getDescriptions().get("en"));
        assertEquals("Deutsche Beschreibung", template.getDescriptions().get("de"));
        assertNotNull(template.getPulledAt());
        assertNotNull(template.getJson());
    }

    @Test
    void fetchTemplates_shouldHandleMultipleTemplates() {
        // --- Arrange ---
        ObjectNode template1 = createValidTemplateItem("Template1", "1", "0");
        ObjectNode template2 = createValidTemplateItem("Template2", "2", "1");
        resultArray.add(template1);
        resultArray.add(template2);

        when(responseSpec.body(JsonNode.class)).thenReturn(mockResponse);

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertEquals(2, result.size());
        assertEquals("Template1", result.get(0).getName());
        assertEquals("Template2", result.get(1).getName());
    }

    @Test
    void fetchTemplates_shouldSkipNonTemplateItems() {
        // --- Arrange ---
        ObjectNode templateItem = createValidTemplateItem("ValidTemplate", "1", "0");
        ObjectNode nonTemplateItem = createItemWithKind("NonTemplate", "SubModel");

        resultArray.add(templateItem);
        resultArray.add(nonTemplateItem);

        when(responseSpec.body(JsonNode.class)).thenReturn(mockResponse);

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertEquals(1, result.size());
        assertEquals("ValidTemplate", result.get(0).getName());
    }

    @Test
    void fetchTemplates_shouldThrowWhenResponseIsNull() {
        // --- Arrange ---
        when(responseSpec.body(JsonNode.class)).thenReturn(null);

        // --- Act & Assert ---
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                smtRepoClient.fetchTemplates());

        assertEquals("empty Response from SMT-Repo", exception.getMessage());
    }

    @Test
    void fetchTemplates_shouldThrowWhenResultIsNotArray() {
        // --- Arrange ---
        ObjectNode invalidResponse = objectMapper.createObjectNode();
        invalidResponse.put("result", "not an array");

        when(responseSpec.body(JsonNode.class)).thenReturn(invalidResponse);

        // --- Act & Assert ---
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                smtRepoClient.fetchTemplates());

        assertEquals("Expects an array of type: 'result'", exception.getMessage());
    }

    @Test
    void fetchTemplates_shouldReturnEmptyListWhenResultArrayIsEmpty() {
        // --- Arrange ---
        when(responseSpec.body(JsonNode.class)).thenReturn(mockResponse);

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchTemplates_shouldSkipTemplatesWithoutAdministration() {
        // --- Arrange ---
        ObjectNode validTemplate = createValidTemplateItem("ValidTemplate", "1", "0");
        ObjectNode templateWithoutAdmin = createTemplateWithoutAdministration("NoAdminTemplate");

        resultArray.add(validTemplate);
        resultArray.add(templateWithoutAdmin);

        when(responseSpec.body(JsonNode.class)).thenReturn(mockResponse);

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertEquals(1, result.size());
        assertEquals("ValidTemplate", result.get(0).getName());
    }

    @Test
    void fetchTemplates_shouldAddMissingValueFields() {
        // --- Arrange ---
        ObjectNode templateItem = createTemplateWithValueTypeButNoValue();
        resultArray.add(templateItem);

        when(responseSpec.body(JsonNode.class)).thenReturn(mockResponse);

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertEquals(1, result.size());
        Template template = result.get(0);

        // Verify that value field was added where valueType exists
        JsonNode json = template.getJson();
        assertTrue(json.has("testField"));
        JsonNode testField = json.get("testField");
        assertTrue(testField.has("valueType"));
        assertTrue(testField.has("value"));
        assertEquals("", testField.get("value").asText());
    }

    // --- Helper Methods ---
    private ObjectNode createValidTemplateItem(String name, String version, String revision) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("idShort", name);
        item.put("kind", "Template");

        // Add descriptions
        ArrayNode descriptions = objectMapper.createArrayNode();
        ObjectNode enDesc = objectMapper.createObjectNode();
        enDesc.put("language", "en");
        enDesc.put("text", "English description");
        descriptions.add(enDesc);

        ObjectNode deDesc = objectMapper.createObjectNode();
        deDesc.put("language", "de");
        deDesc.put("text", "Deutsche Beschreibung");
        descriptions.add(deDesc);

        item.set("description", descriptions);

        // Add administration
        ObjectNode admin = objectMapper.createObjectNode();
        admin.put("version", version);
        admin.put("revision", revision);
        item.set("administration", admin);

        return item;
    }

    private ObjectNode createItemWithKind(String name, String kind) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("idShort", name);
        item.put("kind", kind);

        ObjectNode admin = objectMapper.createObjectNode();
        admin.put("version", "1");
        admin.put("revision", "0");
        item.set("administration", admin);

        return item;
    }

    private ObjectNode createTemplateWithoutAdministration(String name) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("idShort", name);
        item.put("kind", "Template");
        // No administration field
        return item;
    }

    private ObjectNode createTemplateWithValueTypeButNoValue() {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("idShort", "TemplateWithValueType");
        item.put("kind", "Template");

        ObjectNode admin = objectMapper.createObjectNode();
        admin.put("version", "1");
        admin.put("revision", "0");
        item.set("administration", admin);

        // Add a field with valueType but no value
        ObjectNode testField = objectMapper.createObjectNode();
        testField.put("valueType", "xs:string");
        // No "value" field - should be added by the method
        item.set("testField", testField);

        return item;
    }
}