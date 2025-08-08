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

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SMTRepoClientTest {

    @Mock private WebClient webClient;
    @Mock private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private SMTRepoClient smtRepoClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ObjectNode mockResponse;
    private ArrayNode resultArray;

    @BeforeEach
    void setup() {
        // Setup WebClient mock chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
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

        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(mockResponse));

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

        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(mockResponse));

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertEquals(2, result.size());
        assertEquals("Template1", result.get(0).getName());
        assertEquals("Template2", result.get(1).getName());
        assertEquals("1", result.get(0).getVersion());
        assertEquals("2", result.get(1).getVersion());
    }

    @Test
    void fetchTemplates_shouldSkipNonTemplateItems() {
        // --- Arrange ---
        ObjectNode templateItem = createValidTemplateItem("ValidTemplate", "1", "0");
        ObjectNode nonTemplateItem = createItemWithKind("NonTemplate", "SubModel");

        resultArray.add(templateItem);
        resultArray.add(nonTemplateItem);

        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(mockResponse));

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertEquals(1, result.size());
        assertEquals("ValidTemplate", result.get(0).getName());
    }

    @Test
    void fetchTemplates_shouldSkipTemplatesWithoutAdministration() {
        // --- Arrange ---
        ObjectNode validTemplate = createValidTemplateItem("ValidTemplate", "1", "0");
        ObjectNode templateWithoutAdmin = createTemplateWithoutAdministration("NoAdminTemplate");

        resultArray.add(validTemplate);
        resultArray.add(templateWithoutAdmin);

        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(mockResponse));

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertEquals(1, result.size());
        assertEquals("ValidTemplate", result.get(0).getName());
    }

    @Test
    void fetchTemplates_shouldSkipTemplatesWithoutVersion() {
        // --- Arrange ---
        ObjectNode validTemplate = createValidTemplateItem("ValidTemplate", "1", "0");
        ObjectNode templateWithoutVersion = createTemplateWithIncompleteAdministration("NoVersionTemplate", null, "0");

        resultArray.add(validTemplate);
        resultArray.add(templateWithoutVersion);

        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(mockResponse));

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertEquals(1, result.size());
        assertEquals("ValidTemplate", result.get(0).getName());
    }

    @Test
    void fetchTemplates_shouldSkipTemplatesWithoutRevision() {
        // --- Arrange ---
        ObjectNode validTemplate = createValidTemplateItem("ValidTemplate", "1", "0");
        ObjectNode templateWithoutRevision = createTemplateWithIncompleteAdministration("NoRevisionTemplate", "1", null);

        resultArray.add(validTemplate);
        resultArray.add(templateWithoutRevision);

        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(mockResponse));

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertEquals(1, result.size());
        assertEquals("ValidTemplate", result.get(0).getName());
    }

    @Test
    void fetchTemplates_shouldHandleEmptyDescriptions() {
        // --- Arrange ---
        ObjectNode templateItem = createValidTemplateItemWithoutDescriptions("MinimalTemplate", "1", "0");
        resultArray.add(templateItem);

        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(mockResponse));

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertEquals(1, result.size());
        Template template = result.get(0);
        assertEquals("MinimalTemplate", template.getName());
        assertTrue(template.getDescriptions().isEmpty());
        assertEquals("1", template.getVersion());
        assertEquals("0", template.getRevision());
    }

    @Test
    void fetchTemplates_shouldAddMissingValueFields() {
        // --- Arrange ---
        ObjectNode templateItem = createTemplateWithValueTypeButNoValue();
        resultArray.add(templateItem);

        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(mockResponse));

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

    @Test
    void fetchTemplates_shouldHandleNestedValueTypeFields() {
        // --- Arrange ---
        ObjectNode templateItem = createTemplateWithNestedValueTypes();
        resultArray.add(templateItem);

        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(mockResponse));

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertEquals(1, result.size());
        Template template = result.get(0);

        // Verify nested value fields were added
        JsonNode json = template.getJson();
        JsonNode nested = json.get("nested").get("deep").get("field");
        assertTrue(nested.has("valueType"));
        assertTrue(nested.has("value"));
        assertEquals("", nested.get("value").asText());
    }

    @Test
    void fetchTemplates_shouldHandleArraysWithValueTypes() {
        // --- Arrange ---
        ObjectNode templateItem = createTemplateWithArrayValueTypes();
        resultArray.add(templateItem);

        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(mockResponse));

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertEquals(1, result.size());
        Template template = result.get(0);

        // Verify array elements got value fields
        JsonNode json = template.getJson();
        JsonNode arrayElement = json.get("items").get(0);
        assertTrue(arrayElement.has("valueType"));
        assertTrue(arrayElement.has("value"));
    }

    // --- Error Cases ---
    @Test
    void fetchTemplates_shouldThrowWhenResponseIsNull() {
        // --- Arrange ---
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.empty());

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

        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(invalidResponse));

        // --- Act & Assert ---
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                smtRepoClient.fetchTemplates());

        assertEquals("Expects an array of type: 'result'", exception.getMessage());
    }

    @Test
    void fetchTemplates_shouldThrowWhenResultFieldMissing() {
        // --- Arrange ---
        ObjectNode responseWithoutResult = objectMapper.createObjectNode();
        // No "result" field

        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(responseWithoutResult));

        // --- Act & Assert ---
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                smtRepoClient.fetchTemplates());

        assertEquals("Expects an array of type: 'result'", exception.getMessage());
    }

    @Test
    void fetchTemplates_shouldReturnEmptyListWhenResultArrayIsEmpty() {
        // --- Arrange ---
        // resultArray is already empty from setup

        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(mockResponse));

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchTemplates_shouldReturnEmptyListWhenAllItemsAreNonTemplates() {
        // --- Arrange ---
        ObjectNode nonTemplate1 = createItemWithKind("Item1", "SubModel");
        ObjectNode nonTemplate2 = createItemWithKind("Item2", "Asset");
        resultArray.add(nonTemplate1);
        resultArray.add(nonTemplate2);

        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(mockResponse));

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchTemplates_shouldReturnEmptyListWhenAllTemplatesLackAdministration() {
        // --- Arrange ---
        ObjectNode template1 = createTemplateWithoutAdministration("Template1");
        ObjectNode template2 = createTemplateWithIncompleteAdministration("Template2", "1", null);
        ObjectNode template3 = createTemplateWithIncompleteAdministration("Template3", null, "0");

        resultArray.add(template1);
        resultArray.add(template2);
        resultArray.add(template3);

        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(mockResponse));

        // --- Act ---
        List<Template> result = smtRepoClient.fetchTemplates();

        // --- Assert ---
        assertTrue(result.isEmpty());
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

    private ObjectNode createValidTemplateItemWithoutDescriptions(String name, String version, String revision) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("idShort", name);
        item.put("kind", "Template");

        // Add administration but no descriptions
        ObjectNode admin = objectMapper.createObjectNode();
        admin.put("version", version);
        admin.put("revision", revision);
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

    private ObjectNode createTemplateWithIncompleteAdministration(String name, String version, String revision) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("idShort", name);
        item.put("kind", "Template");

        ObjectNode admin = objectMapper.createObjectNode();
        if (version != null) {
            admin.put("version", version);
        }
        if (revision != null) {
            admin.put("revision", revision);
        }
        item.set("administration", admin);

        return item;
    }

    private ObjectNode createItemWithKind(String name, String kind) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("idShort", name);
        item.put("kind", kind);
        return item;
    }

    private ObjectNode createTemplateWithValueTypeButNoValue() {
        ObjectNode item = createValidTemplateItem("TestTemplate", "1", "0");

        ObjectNode testField = objectMapper.createObjectNode();
        testField.put("valueType", "string");
        // Intentionally no "value" field
        item.set("testField", testField);

        return item;
    }

    private ObjectNode createTemplateWithNestedValueTypes() {
        ObjectNode item = createValidTemplateItem("NestedTemplate", "1", "0");

        ObjectNode nested = objectMapper.createObjectNode();
        ObjectNode deep = objectMapper.createObjectNode();
        ObjectNode field = objectMapper.createObjectNode();
        field.put("valueType", "integer");

        deep.set("field", field);
        nested.set("deep", deep);
        item.set("nested", nested);

        return item;
    }

    private ObjectNode createTemplateWithArrayValueTypes() {
        ObjectNode item = createValidTemplateItem("ArrayTemplate", "1", "0");

        ArrayNode items = objectMapper.createArrayNode();
        ObjectNode arrayItem = objectMapper.createObjectNode();
        arrayItem.put("valueType", "boolean");
        items.add(arrayItem);

        item.set("items", items);
        return item;
    }
}