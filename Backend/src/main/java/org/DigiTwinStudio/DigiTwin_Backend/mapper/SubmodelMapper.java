package org.DigiTwinStudio.DigiTwin_Backend.mapper;

import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.mapstruct.Mapper;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.SubmodelDto;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring")
public interface SubmodelMapper {

    SubmodelDto toDto(Submodel submodel);

    Submodel fromDto(SubmodelDto dto);

    /**
     * Factory for creating a concrete Submodel implementation,
     * since Submodel is an interface.
     */
    @ObjectFactory
    default Submodel createSubmodel() {
        return new DefaultSubmodel();
    }
}
