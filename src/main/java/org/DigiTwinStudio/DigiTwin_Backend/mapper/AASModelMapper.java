package org.DigiTwinStudio.DigiTwin_Backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;

@Mapper(componentModel = "spring", uses = PublishMetadataMapper.class)
public interface AASModelMapper {

    AASModelDto toDto(AASModel model);

    /**
     * Converts an AASModelDto to a domain AASModel.
     * The ownerId is not provided by the client and therefore ignored here;
     * it must be set separately (e.g., from the authenticated user's context).
     */
    @Mapping(target = "ownerId", ignore = true)
    AASModel fromDto(AASModelDto dto, String ownerId);
}
