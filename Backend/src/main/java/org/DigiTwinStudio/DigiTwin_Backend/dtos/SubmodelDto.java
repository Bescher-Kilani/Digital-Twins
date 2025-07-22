package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.*;

import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmodelDto {


    private DefaultSubmodel submodel;
}
