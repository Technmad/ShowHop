package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.showhop.api.dto.TicketTypeRequestDto;
import com.showhop.api.entity.Event;
import com.showhop.api.entity.TicketType;
import com.showhop.api.entity.enums.EventStatus;
import com.showhop.api.exception.EventNotFoundException;
import com.showhop.api.exception.TicketTypeNotFoundException;
import com.showhop.api.mapper.TicketTypeMapper;
import com.showhop.api.repository.EventRepository;
import com.showhop.api.repository.TicketTypeRepository;
import com.showhop.api.service.impl.TicketTypeServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class TicketTypeServiceImplTest {

  @Mock
  private TicketTypeRepository ticketTypeRepository;
  @Mock
  private EventRepository eventRepository;
  @Mock
  private TicketTypeMapper ticketTypeMapper;

  @InjectMocks
  private TicketTypeServiceImpl ticketTypeService;

  @Test
  void createTicketTypeAttachesItToAnEventTheOrganizerOwns() {
    UUID organizerId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    Event event = Event.builder().id(eventId).build();
    TicketTypeRequestDto request = aRequest();
    TicketType mapped = new TicketType();

    when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
        .thenReturn(Optional.of(event));
    when(ticketTypeMapper.toEntity(request)).thenReturn(mapped);
    when(ticketTypeRepository.save(mapped)).thenReturn(mapped);

    TicketType result = ticketTypeService.createTicketType(organizerId, eventId, request);

    assertThat(result.getEvent()).isSameAs(event);
  }

  @Test
  void createTicketTypeRejectsAnEventTheOrganizerDoesNotOwn() {
    UUID organizerId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> ticketTypeService.createTicketType(organizerId, eventId, aRequest()))
        .isInstanceOf(EventNotFoundException.class);
  }

  @Test
  void updateTicketTypeFailsWhenTheTicketTypeDoesNotBelongToTheEvent() {
    UUID organizerId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    UUID ticketTypeId = UUID.randomUUID();
    when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
        .thenReturn(Optional.of(Event.builder().id(eventId).build()));
    when(ticketTypeRepository.findByIdAndEventId(ticketTypeId, eventId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> ticketTypeService.updateTicketType(
        organizerId, eventId, ticketTypeId, aRequest()))
        .isInstanceOf(TicketTypeNotFoundException.class);
  }

  @Test
  void deleteTicketTypeIsANoOpWhenNothingMatches() {
    UUID organizerId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    UUID ticketTypeId = UUID.randomUUID();
    when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
        .thenReturn(Optional.of(Event.builder().id(eventId).build()));
    when(ticketTypeRepository.findByIdAndEventId(ticketTypeId, eventId))
        .thenReturn(Optional.empty());

    ticketTypeService.deleteTicketType(organizerId, eventId, ticketTypeId);

    verify(ticketTypeRepository, org.mockito.Mockito.never()).delete(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void listTicketTypesForPublishedEventRejectsAnUnpublishedEvent() {
    UUID eventId = UUID.randomUUID();
    when(eventRepository.findByIdAndStatus(eventId, EventStatus.PUBLISHED))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() ->
        ticketTypeService.listTicketTypesForPublishedEvent(eventId, PageRequest.of(0, 10)))
        .isInstanceOf(EventNotFoundException.class);
  }

  private TicketTypeRequestDto aRequest() {
    return new TicketTypeRequestDto("General Admission", null, new BigDecimal("29.99"), 200);
  }
}
