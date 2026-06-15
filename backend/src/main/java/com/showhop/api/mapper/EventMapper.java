package com.showhop.api.mapper;

import com.showhop.api.dto.EventRequestDto;
import com.showhop.api.dto.EventResponseDto;
import com.showhop.api.dto.PublishedEventResponseDto;
import com.showhop.api.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EventMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "organizer", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Event toEntity(EventRequestDto dto);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "organizer", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntityFromDto(EventRequestDto dto, @MappingTarget Event event);

  @Mapping(target = "organizerId", source = "organizer.id")
  EventResponseDto toResponseDto(Event event);

  PublishedEventResponseDto toPublishedResponseDto(Event event);
}
