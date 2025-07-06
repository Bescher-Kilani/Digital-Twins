package org.DigiTwinStudio.DigiTwin_Backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Template;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.TemplateDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.ExternalTemplateDto;

@Mapper(componentModel = "spring")
public interface TemplateMapper {

    TemplateDto toDto(Template template);

    Template fromDto(TemplateDto dto);

    @Mapping(target = "json", source = "rawJson")
    @Mapping(target = "pulledAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "active", constant = "true")
    Template fromExternal(ExternalTemplateDto ext);
}
