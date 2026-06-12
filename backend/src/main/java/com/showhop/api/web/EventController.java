package com.showhop.api.web;

import static com.showhop.api.security.JwtUtil.parseUserId;

import com.showhop.api.dto.EventRequestDto;
import com.showhop.api.dto.EventResponseDto;
import com.showhop.api.entity.Event;
import com.showhop.api.exception.EventNotFoundException;
import com.showhop.api.mapper.EventMapper;
import com.showhop.api.service.EventService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

  private final EventService eventService;
  private final EventMapper eventMapper;

  @PostMapping
  public ResponseEntity<EventResponseDto> createEvent(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody EventRequestDto request) {
    Event event = eventService.createEvent(parseUserId(jwt), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(eventMapper.toResponseDto(event));
  }

  @GetMapping
  public Page<EventResponseDto> listEvents(
      @AuthenticationPrincipal Jwt jwt, Pageable pageable) {
    return eventService.listEventsForOrganizer(parseUserId(jwt), pageable)
        .map(eventMapper::toResponseDto);
  }

  @GetMapping("/{eventId}")
  public EventResponseDto getEvent(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
    return eventService.getEventForOrganizer(parseUserId(jwt), eventId)
        .map(eventMapper::toResponseDto)
        .orElseThrow(() -> new EventNotFoundException(
            "Event with id '%s' was not found".formatted(eventId)));
  }

  @PutMapping("/{eventId}")
  public EventResponseDto updateEvent(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID eventId,
      @Valid @RequestBody EventRequestDto request) {
    Event event = eventService.updateEventForOrganizer(parseUserId(jwt), eventId, request);
    return eventMapper.toResponseDto(event);
  }

  @DeleteMapping("/{eventId}")
  public ResponseEntity<Void> deleteEvent(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
    eventService.deleteEventForOrganizer(parseUserId(jwt), eventId);
    return ResponseEntity.noContent().build();
  }
}
