package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for tag information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagDto {

    private String id;

    private String name;

    private String category;

    private int usageCount;
}
