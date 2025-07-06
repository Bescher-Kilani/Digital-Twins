package org.DigiTwinStudio.DigiTwin_Backend.mapper;

import org.mapstruct.Mapper;
import org.DigiTwinStudio.DigiTwin_Backend.domain.PublishMetadata;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.PublishMetadataDto;

@Mapper(componentModel = "spring")
public interface PublishMetadataMapper {
    PublishMetadataDto toDto(PublishMetadata entity);
    PublishMetadata fromDto(PublishMetadataDto dto);
}
