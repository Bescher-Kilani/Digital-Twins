package org.DigiTwinStudio.DigiTwin_Backend.mapper;

import org.mapstruct.Mapper;

import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.TagDto;

/**
 * Maps between Tag entities and their DTOs.
 */
@Mapper(componentModel = "spring")
public interface TagMapper {

    /**
     * Converts a Tag entity to a TagDto.
     *
     * @param tag the Tag entity
     * @return the DTO
     */
    TagDto toDto(Tag tag);

    /**
     * Converts a TagDto to a Tag entity.
     *
     * @param dto the Tag DTO
     * @return the entity
     */
    Tag fromDto(TagDto dto);
}
