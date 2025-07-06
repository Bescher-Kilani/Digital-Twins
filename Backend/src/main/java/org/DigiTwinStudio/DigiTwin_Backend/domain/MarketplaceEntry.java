package org.DigiTwinStudio.DigiTwin_Backend.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document("marketplaceEntries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceEntry {

    @Id
    private String id;

    private String name; // New. Visible name of the entry in the marketplace

    private String author; // visible author name can be a user or organization

    private String shortDescription;

    private List<String> tagIds;

    private LocalDateTime publishedAt;

    private int downloadCount;
}
