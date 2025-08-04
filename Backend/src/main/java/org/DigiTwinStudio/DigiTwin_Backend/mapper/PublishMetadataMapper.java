package org.DigiTwinStudio.DigiTwin_Backend.mapper;

import org.mapstruct.Mapper;

import org.DigiTwinStudio.DigiTwin_Backend.domain.PublishMetadata;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.PublishMetadataDto;

/**
 * Maps between PublishMetadata entities and their DTOs.
 */
@Mapper(componentModel = "spring")
public interface PublishMetadataMapper {

    /**
     * Converts a domain entity to its DTO.
     *
     * @param entity the publish metadata entity
     * @return the DTO
     */
    PublishMetadataDto toDto(PublishMetadata entity);

    /**
     * Converts a DTO to a domain entity.
     *
     * @param dto the publish metadata DTO
     * @return the domain entity
     */
    PublishMetadata fromDto(PublishMetadataDto dto);
}
