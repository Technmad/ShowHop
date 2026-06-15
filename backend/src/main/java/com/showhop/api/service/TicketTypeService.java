package com.showhop.api.service;

import com.showhop.api.dto.TicketTypeRequestDto;
import com.showhop.api.entity.TicketType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketTypeService {

  TicketType createTicketType(UUID organizerId, UUID eventId, TicketTypeRequestDto request);

  Page<TicketType> listTicketTypesForEvent(UUID organizerId, UUID eventId, Pageable pageable);

  Optional<TicketType> getTicketType(UUID organizerId, UUID eventId, UUID ticketTypeId);

  TicketType updateTicketType(
      UUID organizerId, UUID eventId, UUID ticketTypeId, TicketTypeRequestDto request);

  void deleteTicketType(UUID organizerId, UUID eventId, UUID ticketTypeId);
}
