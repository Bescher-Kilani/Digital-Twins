package org.DigiTwinStudio.DigiTwin_Backend.domain;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.*;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity for a template with metadata, descriptions, and JSON content.
 */
@Document("templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template {

    @Id
    private String id;

    private String name;

    // key = language, value = description
    private Map<String, String> descriptions;

    private String version;

    private String revision;

    private JsonNode json;

    private LocalDateTime pulledAt;
}
