package org.DigiTwinStudio.DigiTwin_Backend.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template {

    @Id
    private String id;

    private String name;

    private String description;

    private String version;

    private String json;

    private byte[] documentationAsPdf;

    private String sourceUrl;

    private LocalDateTime pulledAt;

    private boolean active; // Indicates if the template is currently visible or not. (if not, there is a newer version available)
}
