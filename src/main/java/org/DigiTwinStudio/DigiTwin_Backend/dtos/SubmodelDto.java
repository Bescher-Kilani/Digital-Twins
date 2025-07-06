package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.*;

import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmodelDto {


    private Submodel submodel;
}
