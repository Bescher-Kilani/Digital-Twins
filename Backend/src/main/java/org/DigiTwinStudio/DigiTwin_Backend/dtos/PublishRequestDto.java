package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishRequestDto {

    private String author;

    private String shortDescription;

    private List<String> tagIds;
}
