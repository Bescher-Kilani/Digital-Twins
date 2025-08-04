package org.DigiTwinStudio.DigiTwin_Backend.mapper;

import org.mapstruct.Mapper;

import org.DigiTwinStudio.DigiTwin_Backend.domain.MarketplaceEntry;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceEntryDto;

/**
 * Maps between MarketplaceEntry entities and their DTOs.
 */
@Mapper(componentModel = "spring")
public interface MarketplaceMapper {

    /**
     * Converts a domain entry to its DTO.
     *
     * @param entry the marketplace entry
     * @return the DTO
     */
    MarketplaceEntryDto toDto(MarketplaceEntry entry);
}
