package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.*;

import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;

/**
 * DTO for transferring a DefaultSubmodel.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmodelDto {

    private DefaultSubmodel submodel;
}
