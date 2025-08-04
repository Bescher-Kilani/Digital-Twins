package org.DigiTwinStudio.DigiTwin_Backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;

/**
 * Maps between AASModel entities and their DTOs.
 */
@Mapper(componentModel = "spring", uses = PublishMetadataMapper.class)
public interface AASModelMapper {

    /**
     * Converts a domain model to its DTO.
     *
     * @param model the domain model
     * @return the DTO
     */
    AASModelDto toDto(AASModel model);

    /**
     * Converts a DTO to a domain model. Ignores ownerId from the DTO.
     *
     * @param dto the DTO
     * @param ownerId the owner to set on the domain model
     * @return the domain model
     */
    @Mapping(target = "ownerId", ignore = true)
    AASModel fromDto(AASModelDto dto, String ownerId);
}
