package com.showhop.api.mapper;

import com.showhop.api.dto.TicketTypeRequestDto;
import com.showhop.api.dto.TicketTypeResponseDto;
import com.showhop.api.entity.TicketType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TicketTypeMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "event", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  TicketType toEntity(TicketTypeRequestDto dto);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "event", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntityFromDto(TicketTypeRequestDto dto, @MappingTarget TicketType ticketType);

  @Mapping(target = "eventId", source = "event.id")
  TicketTypeResponseDto toResponseDto(TicketType ticketType);
}
