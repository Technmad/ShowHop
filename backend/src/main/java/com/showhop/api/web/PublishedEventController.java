package com.showhop.api.web;

import com.showhop.api.dto.PublishedEventResponseDto;
import com.showhop.api.dto.TicketTypeResponseDto;
import com.showhop.api.entity.Event;
import com.showhop.api.exception.EventNotFoundException;
import com.showhop.api.mapper.EventMapper;
import com.showhop.api.mapper.TicketTypeMapper;
import com.showhop.api.service.EventService;
import com.showhop.api.service.TicketTypeService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, unauthenticated browsing of published events -- no organizer scoping. */
@RestController
@RequestMapping("/api/v1/published-events")
@RequiredArgsConstructor
public class PublishedEventController {

  private final EventService eventService;
  private final EventMapper eventMapper;
  private final TicketTypeService ticketTypeService;
  private final TicketTypeMapper ticketTypeMapper;

  @GetMapping
  public Page<PublishedEventResponseDto> listPublishedEvents(
      @RequestParam(required = false) String q, Pageable pageable) {
    Page<Event> events = (q == null || q.isBlank())
        ? eventService.listPublishedEvents(pageable)
        : eventService.searchPublishedEvents(q, pageable);
    return events.map(eventMapper::toPublishedResponseDto);
  }

  @GetMapping("/{eventId}")
  public PublishedEventResponseDto getPublishedEvent(@PathVariable UUID eventId) {
    return eventService.getPublishedEvent(eventId)
        .map(eventMapper::toPublishedResponseDto)
        .orElseThrow(() -> new EventNotFoundException(
            "Event with id '%s' was not found".formatted(eventId)));
  }

  @GetMapping("/{eventId}/ticket-types")
  public Page<TicketTypeResponseDto> listTicketTypes(
      @PathVariable UUID eventId, Pageable pageable) {
    return ticketTypeService.listTicketTypesForPublishedEvent(eventId, pageable)
        .map(ticketTypeMapper::toResponseDto);
  }
}
