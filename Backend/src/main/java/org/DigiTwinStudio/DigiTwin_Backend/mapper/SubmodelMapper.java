package org.DigiTwinStudio.DigiTwin_Backend.mapper;

import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.mapstruct.Mapper;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.SubmodelDto;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring")
public interface SubmodelMapper {

    SubmodelDto toDto(DefaultSubmodel submodel);

    DefaultSubmodel fromDto(SubmodelDto dto);


}
