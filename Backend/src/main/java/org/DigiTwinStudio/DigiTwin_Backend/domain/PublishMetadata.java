package org.DigiTwinStudio.DigiTwin_Backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Metadata for published AAS models, including author, description, tags, and publish time.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishMetadata {

    // visible author name can be a user or organization
    String author;

    // description visible in the Marketplace
    String shortDescription;

    // references to Tag objects
    List<String> tagIds;

    LocalDateTime publishedAt;
}
