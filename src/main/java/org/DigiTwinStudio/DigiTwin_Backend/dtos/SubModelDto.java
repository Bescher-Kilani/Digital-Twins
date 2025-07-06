package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.*;

import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubModelDto {


    private Submodel submodel;
}
