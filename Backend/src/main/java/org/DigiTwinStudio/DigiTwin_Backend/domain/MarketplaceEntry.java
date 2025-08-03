package org.DigiTwinStudio.DigiTwin_Backend.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TextScore;

import java.time.LocalDateTime;
import java.util.List;

@Document("marketplaceEntries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceEntry {

    @Id
    private String id; // ID of referenced Model

    @TextIndexed
    private String name; // New. Visible name of the entry in the marketplace

    @TextIndexed
    private String author; // visible author name can be a user or organization

    @TextIndexed
    private String shortDescription;

    @Indexed
    private List<String> tagIds;

    @Indexed(direction = IndexDirection.DESCENDING) // newest first
    private LocalDateTime publishedAt;

    private int downloadCount;

    @TextScore
    private Float score; // relevance for search
}
