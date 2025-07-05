package org.DigiTwinStudio.DigiTwin_Backend.domain;

import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
public class PublishMetadata {
    String author; // visible author name can be a user or organization
    String shortDescription; // description visible in the Marketplace
    List<String> tagIds; // references to Tag objects
    LocalDateTime publishedAt;
}
