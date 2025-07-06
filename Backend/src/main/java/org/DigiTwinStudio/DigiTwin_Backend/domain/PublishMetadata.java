package org.DigiTwinStudio.DigiTwin_Backend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublishMetadata {
    String author; // visible author name can be a user or organization
    String shortDescription; // description visible in the Marketplace
    List<String> tagIds; // references to Tag objects
    LocalDateTime publishedAt;
}
