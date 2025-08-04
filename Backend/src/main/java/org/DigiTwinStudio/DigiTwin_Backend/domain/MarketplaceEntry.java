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

/**
 * Entity for a marketplace entry referencing an AAS model.
 */
@Document("marketplaceEntries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceEntry {

    // ID of referenced Model
    @Id
    private String id;

    // New visible name of the entry in the marketplace
    @TextIndexed
    private String name;

    // visible author name can be a user or organization
    @TextIndexed
    private String author;

    @TextIndexed
    private String shortDescription;

    @Indexed
    private List<String> tagIds;

    // newest first
    @Indexed(direction = IndexDirection.DESCENDING)
    private LocalDateTime publishedAt;

    private int downloadCount;

    // relevance for search
    @TextScore
    private Float score;
}
