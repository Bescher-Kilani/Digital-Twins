package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for publish metadata of an AAS model.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishMetadataDto {

    private String author;

    private String shortDescription;

    private List<String> tagIds;

    private LocalDateTime publishedAt;
}
