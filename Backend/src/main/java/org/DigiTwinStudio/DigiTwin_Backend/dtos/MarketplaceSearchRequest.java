package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceSearchRequest {
    private String searchText;              // text (name/description/author)
    private LocalDateTime publishedAfter;   // "newer than"
    private List<String> tagIds;
}
