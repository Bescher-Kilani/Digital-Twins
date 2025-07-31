package org.DigiTwinStudio.DigiTwin_Backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Template;
import org.DigiTwinStudio.DigiTwin_Backend.utils.DateTimeUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SMTRepoClient {
    private final WebClient webClient; // Gets injected. See config "WebClientConfig"

    /**
     * Pulls all templates from the submodel-repo
     * @return List of all Templates
     */
    public List<Template> fetchTemplates() {
        log.info("fetchTemplates");
        JsonNode root = webClient.get()
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (root == null) {
            throw new IllegalStateException("empty Response from SMT-Repo");
        }
        log.info("got Response from SMT-Repo.");
        // get Response-JSON Result-Array
        JsonNode resultArray = root.path("result");
        if (!resultArray.isArray()) {
            throw new IllegalStateException("Expects an array of type: 'result'");
        }
        log.info("Response is valid.");
        // Extract templates out of Response-JSON
        List<Template> templates = new ArrayList<>();
        // check all result-entries
        for (JsonNode item : resultArray) {
            // Name
            String name = item.path("idShort").asText();
            log.info("Item: {}", name);

            // only "Templates" are relevant
            if (!"Template".equalsIgnoreCase(item.path("kind").asText())) {
                log.info("Skipping because of kind: {}", item.path("kind").asText());
                continue;
            }

            // Descriptions
            Map<String, String> descriptions = new LinkedHashMap<>();
            JsonNode descArray = item.path("description");
            if (descArray.isArray()) {
                for (JsonNode d : descArray) {
                    descriptions.put(
                            d.path("language").asText(),
                            d.path("text").asText()
                    );
                }
                log.info("Found {} descriptions.", descriptions.size());
            }

            // Version.Revision from administration
            String version = "";
            String revision = "";
            JsonNode admin = item.path("administration");
            if (admin.hasNonNull("version") && admin.hasNonNull("revision")) {
                version = admin.path("version").asText();
                revision = admin.path("revision").asText();
                log.info("Found version.Revision: {}.{}", version, revision);
            }

            // JSON

            // Constructing Template-domain-object using Lombok-Builder. No ID so MongoDB generates one
            // call ensureValueFields on JSON to add missing "value" fields
            ensureValueFields(item);
            Template template = Template.builder()
                    .name(name)
                    .descriptions(descriptions)
                    .version(version)
                    .revision(revision)
                    .json(item)
                    .pulledAt(DateTimeUtil.nowUtc())
                    .active(true)
                    .build();
            log.info("Created template for {}", name);
            templates.add(template);
        }
        return templates;
    }

    /**
     * Recursively traverses the given JsonNode tree and ensures that every ObjectNode
     * containing a "valueType" field also contains a "value" field.
     * If an ObjectNode has "valueType" but lacks "value", this method adds
     * a "value" field with an empty string ("") as its value.
     *
     * The traversal covers all nested objects and arrays within the JSON structure.
     *
     * @param node the root JsonNode to process; can be an ObjectNode, ArrayNode, or any JsonNode
     */
    private void ensureValueFields(JsonNode node) {
        if (node == null) return;

        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            if (obj.has("valueType") && !obj.has("value")) {
                // Add empty "value" field
                obj.put("value", "");
                log.debug("Added missing value element");
            }
            // Recurse into all fields
            obj.forEach(this::ensureValueFields);
        } else if (node.isArray()) {
            // convert to ObjectNode
            for (JsonNode elem : node) {
                ensureValueFields(elem);
            }
        }
    }
}
