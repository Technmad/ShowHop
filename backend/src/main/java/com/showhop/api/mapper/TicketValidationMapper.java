package com.showhop.api.mapper;

import com.showhop.api.dto.TicketValidationResponseDto;
import com.showhop.api.entity.TicketValidation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketValidationMapper {

  @Mapping(target = "ticketId", source = "ticket.id")
  @Mapping(target = "validatedById", source = "validatedBy.id")
  TicketValidationResponseDto toResponseDto(TicketValidation ticketValidation);
}
