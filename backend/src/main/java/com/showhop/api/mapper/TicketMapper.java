package com.showhop.api.mapper;

import com.showhop.api.dto.TicketResponseDto;
import com.showhop.api.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketMapper {

  @Mapping(target = "ticketTypeId", source = "ticketType.id")
  @Mapping(target = "eventId", source = "ticketType.event.id")
  TicketResponseDto toResponseDto(Ticket ticket);
}
