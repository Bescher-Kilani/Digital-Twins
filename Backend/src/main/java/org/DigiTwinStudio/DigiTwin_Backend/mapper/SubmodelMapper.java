package org.DigiTwinStudio.DigiTwin_Backend.mapper;

import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;

import org.mapstruct.Mapper;

import org.DigiTwinStudio.DigiTwin_Backend.dtos.SubmodelDto;

/**
 * Maps between submodel DTOs and DefaultSubmodel entities.
 */
@Mapper(componentModel = "spring")
public interface SubmodelMapper {

    /**
     * Converts a DefaultSubmodel to a SubmodelDto.
     *
     * @param submodel the domain submodel
     * @return the DTO
     */
    SubmodelDto toDto(DefaultSubmodel submodel);

    /**
     * Converts a SubmodelDto to a DefaultSubmodel.
     *
     * @param dto the submodel DTO
     * @return the domain submodel
     */
    DefaultSubmodel fromDto(SubmodelDto dto);
}
