package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for template information and metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateDto {

    private String id;

    private String name;

    private Map<String, String> descriptions; // key = language, value = description

    private String version;

    private String revision;

    private JsonNode json;

    private LocalDateTime pulledAt;
}
