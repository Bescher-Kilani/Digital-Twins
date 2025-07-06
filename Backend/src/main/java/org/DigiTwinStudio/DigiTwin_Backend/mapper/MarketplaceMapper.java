package org.DigiTwinStudio.DigiTwin_Backend.mapper;

import org.mapstruct.Mapper;
import org.DigiTwinStudio.DigiTwin_Backend.domain.MarketplaceEntry;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceEntryDto;
import java.util.List;

@Mapper(componentModel = "spring")
public interface MarketplaceMapper {

    MarketplaceEntryDto toDto(MarketplaceEntry entry);

    List<MarketplaceEntryDto> toDtoList(List<MarketplaceEntry> entries);
}
