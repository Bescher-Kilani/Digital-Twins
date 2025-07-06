package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalTemplateDto {

    private String name;

    private String description;

    private String sourceUrl;

    private String rawJson;
}
