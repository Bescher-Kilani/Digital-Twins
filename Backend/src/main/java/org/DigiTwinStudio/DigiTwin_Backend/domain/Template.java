package org.DigiTwinStudio.DigiTwin_Backend.domain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document("templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template {

    @Id
    private String id;

    private String name;

    private Map<String, String> descriptions; // key = language, value = description

    private String version;

    private String revision;

    private JsonNode json;

    private LocalDateTime pulledAt;
}
