package org.DigiTwinStudio.DigiTwin_Backend.mapper;

import org.mapstruct.Mapper;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Template;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.TemplateDto;

@Mapper(componentModel = "spring")
public interface TemplateMapper {

    TemplateDto toDto(Template template);

    Template fromDto(TemplateDto dto);
}
