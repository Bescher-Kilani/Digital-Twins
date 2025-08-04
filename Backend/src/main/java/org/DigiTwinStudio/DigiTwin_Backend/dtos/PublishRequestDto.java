package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.*;

import java.util.List;

/**
 * DTO for a publish request with author, description, and tags.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishRequestDto {

    private String author;

    private String shortDescription;

    private List<String> tagIds;
}
