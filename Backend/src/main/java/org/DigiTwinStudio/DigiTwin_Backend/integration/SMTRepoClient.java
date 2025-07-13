package org.DigiTwinStudio.DigiTwin_Backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
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
public class SMTRepoClient {
    private final WebClient webClient; // Gets injected. See config "WebClientConfig"

    /**
     * Pulls all templates from the submodel-repo
     * @return List of all Templates
     */
    public List<Template> fetchTemplates() {
        JsonNode root = webClient.get()
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (root == null) {
            throw new IllegalStateException("empty Response from SMT-Repo");
        }
        // get Response-JSON
        JsonNode resultArray = root.path("result");
        if (!resultArray.isArray()) {
            throw new IllegalStateException("Expects an array of type: 'result'");
        }

        // Extract templates out of Response-JSON
        List<Template> templates = new ArrayList<>();
        // check all result-entries
        for (JsonNode item : resultArray) {
            // only "Templates" are relevant
            if (!"Template".equalsIgnoreCase(item.path("kind").asText())) {
                continue;
            }
            // Name
            String name = item.path("idShort").asText();

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
            }

            // Version.Revision from administration
            String version = "";
            JsonNode admin = item.path("administration");
            if (admin.hasNonNull("version") && admin.hasNonNull("revision")) {
                version = admin.path("version").asText()
                        + "." + admin.path("revision").asText();
            }

            // Constructing Template-domain-object using Lombok-Builder. No ID so MongoDB generates one
            Template template = Template.builder()
                    .name(name)
                    .descriptions(descriptions)
                    .version(version)
                    .json(item)
                    .pulledAt(DateTimeUtil.nowUtc())
                    .active(true)
                    .build();

            templates.add(template);
        }
        return templates;
    }
}
