package com.showhop.api.service.impl;

import com.showhop.api.dto.TicketTypeRequestDto;
import com.showhop.api.entity.Event;
import com.showhop.api.entity.TicketType;
import com.showhop.api.exception.EventNotFoundException;
import com.showhop.api.exception.TicketTypeNotFoundException;
import com.showhop.api.mapper.TicketTypeMapper;
import com.showhop.api.repository.EventRepository;
import com.showhop.api.repository.TicketTypeRepository;
import com.showhop.api.service.TicketTypeService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketTypeServiceImpl implements TicketTypeService {

  private final TicketTypeRepository ticketTypeRepository;
  private final EventRepository eventRepository;
  private final TicketTypeMapper ticketTypeMapper;

  @Override
  @Transactional
  public TicketType createTicketType(UUID organizerId, UUID eventId, TicketTypeRequestDto request) {
    Event event = eventOwnedBy(organizerId, eventId);

    TicketType ticketType = ticketTypeMapper.toEntity(request);
    ticketType.setEvent(event);

    return ticketTypeRepository.save(ticketType);
  }

  @Override
  public Page<TicketType> listTicketTypesForEvent(
      UUID organizerId, UUID eventId, Pageable pageable) {
    eventOwnedBy(organizerId, eventId);
    return ticketTypeRepository.findByEventId(eventId, pageable);
  }

  @Override
  public Optional<TicketType> getTicketType(UUID organizerId, UUID eventId, UUID ticketTypeId) {
    eventOwnedBy(organizerId, eventId);
    return ticketTypeRepository.findByIdAndEventId(ticketTypeId, eventId);
  }

  @Override
  @Transactional
  public TicketType updateTicketType(
      UUID organizerId, UUID eventId, UUID ticketTypeId, TicketTypeRequestDto request) {
    eventOwnedBy(organizerId, eventId);

    TicketType ticketType = ticketTypeRepository.findByIdAndEventId(ticketTypeId, eventId)
        .orElseThrow(() -> new TicketTypeNotFoundException(
            "Ticket type with id '%s' was not found".formatted(ticketTypeId)));

    ticketTypeMapper.updateEntityFromDto(request, ticketType);

    return ticketTypeRepository.save(ticketType);
  }

  @Override
  @Transactional
  public void deleteTicketType(UUID organizerId, UUID eventId, UUID ticketTypeId) {
    eventOwnedBy(organizerId, eventId);
    ticketTypeRepository.findByIdAndEventId(ticketTypeId, eventId)
        .ifPresent(ticketTypeRepository::delete);
  }

  private Event eventOwnedBy(UUID organizerId, UUID eventId) {
    return eventRepository.findByIdAndOrganizerId(eventId, organizerId)
        .orElseThrow(() -> new EventNotFoundException(
            "Event with id '%s' was not found".formatted(eventId)));
  }
}
