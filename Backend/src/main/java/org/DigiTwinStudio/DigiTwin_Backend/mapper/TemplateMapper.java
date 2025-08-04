package org.DigiTwinStudio.DigiTwin_Backend.mapper;

import org.mapstruct.Mapper;

import org.DigiTwinStudio.DigiTwin_Backend.domain.Template;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.TemplateDto;

/**
 * Maps between Template entities and their DTOs.
 */
@Mapper(componentModel = "spring")
public interface TemplateMapper {

    /**
     * Converts a Template entity to a TemplateDto.
     *
     * @param template the Template entity
     * @return the DTO
     */
    TemplateDto toDto(Template template);

    /**
     * Converts a TemplateDto to a Template entity.
     *
     * @param dto the Template DTO
     * @return the entity
     */
    Template fromDto(TemplateDto dto);
}
