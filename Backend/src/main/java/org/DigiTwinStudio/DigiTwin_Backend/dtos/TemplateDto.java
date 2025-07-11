package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateDto {

    private String id;

    private String name;

    private String json;

    private String description;

    private String sourceUrl;

    private LocalDateTime pulledAt;
}
