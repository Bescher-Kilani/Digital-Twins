package org.DigiTwinStudio.DigiTwin_Backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.DigiTwinStudio.DigiTwin_Backend.domain.Template;
import org.DigiTwinStudio.DigiTwin_Backend.utils.DateTimeUtil;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for fetching and processing templates from the external SMT (Sub-Model-Template) repository.
 * Uses RestClient instead of WebClient to avoid Netty event loop keeping the service awake.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SMTRepoClient {
    private final RestClient restClient;

    /**
     * Fetches all templates from the SMT repository API.
     *
     * @return list of templates
     */
    public List<Template> fetchTemplates() {
        log.info("fetchTemplates");

        JsonNode root = restClient.get()
                .retrieve()
                .body(JsonNode.class);

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

            // Version and Revision from administration
            JsonNode admin = item.path("administration");
            if (admin.isMissingNode() || admin.isNull()) {
                log.info("Skipping because of missing administration.");
                continue;
            }

            String version = admin.path("version").asText(null);
            String revision = admin.path("revision").asText(null);

            if (version == null || revision == null) {
                log.info("Skipping because of missing version or revision.");
                continue;
            }
            log.info("Found version.Revision: {}.{}", version, revision);

            // Deep copy and add missing "value" fields
            JsonNode enrichedJson = addMissingValueFields(item.deepCopy());

            Template template = Template.builder()
                    .name(name)
                    .descriptions(descriptions)
                    .version(version)
                    .revision(revision)
                    .pulledAt(DateTimeUtil.nowUtc())
                    .json(enrichedJson)
                    .build();

            templates.add(template);
            log.info("Created template for {}", name);
        }

        return templates;
    }

    /**
     * Recursively traverses a JSON tree and adds an empty "value" field
     * to any object that has "valueType" but lacks "value".
     *
     * @param node the root node to process
     * @return the same node (mutated in place for ObjectNodes)
     */
    private JsonNode addMissingValueFields(JsonNode node) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            if (obj.has("valueType") && !obj.has("value")) {
                obj.put("value", "");
            }
            obj.fields().forEachRemaining(entry -> addMissingValueFields(entry.getValue()));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                addMissingValueFields(child);
            }
        }
        return node;
    }
}