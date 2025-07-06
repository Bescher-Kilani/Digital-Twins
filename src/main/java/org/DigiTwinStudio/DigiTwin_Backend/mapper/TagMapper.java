package org.DigiTwinStudio.DigiTwin_Backend.mapper;

import org.mapstruct.Mapper;
import java.util.List;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.TagDto;

@Mapper(componentModel = "spring")
public interface TagMapper {
    TagDto toDto(Tag tag);
    Tag fromDto(TagDto dto);
    List<TagDto> toDtoList(List<Tag> tags);
}
