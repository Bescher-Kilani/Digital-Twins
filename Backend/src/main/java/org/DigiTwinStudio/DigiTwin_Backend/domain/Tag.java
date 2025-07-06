package org.DigiTwinStudio.DigiTwin_Backend.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    private String id;

    private String name;

    private String category; // Category of the tag, e.g., "Industry", "Technology", "Sustainability"

    private int usageCount; // Number of times this tag has been used in Marketplace entries
}
