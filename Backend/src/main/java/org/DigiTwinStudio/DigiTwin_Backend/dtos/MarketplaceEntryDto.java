package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data transfer object for marketplace entry information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceEntryDto {

    private String id;

    private String author;

    private String shortDescription;

    private List<String> tagIds;

    private LocalDateTime publishedAt;
}
