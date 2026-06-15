package com.showhop.api.web;

import static com.showhop.api.security.JwtUtil.parseUserId;

import com.showhop.api.dto.TicketTypeRequestDto;
import com.showhop.api.dto.TicketTypeResponseDto;
import com.showhop.api.entity.TicketType;
import com.showhop.api.exception.TicketTypeNotFoundException;
import com.showhop.api.mapper.TicketTypeMapper;
import com.showhop.api.service.TicketTypeService;
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
@RequestMapping("/api/v1/events/{eventId}/ticket-types")
@RequiredArgsConstructor
public class TicketTypeController {

  private final TicketTypeService ticketTypeService;
  private final TicketTypeMapper ticketTypeMapper;

  @PostMapping
  public ResponseEntity<TicketTypeResponseDto> createTicketType(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID eventId,
      @Valid @RequestBody TicketTypeRequestDto request) {
    TicketType ticketType =
        ticketTypeService.createTicketType(parseUserId(jwt), eventId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ticketTypeMapper.toResponseDto(ticketType));
  }

  @GetMapping
  public Page<TicketTypeResponseDto> listTicketTypes(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId, Pageable pageable) {
    return ticketTypeService.listTicketTypesForEvent(parseUserId(jwt), eventId, pageable)
        .map(ticketTypeMapper::toResponseDto);
  }

  @GetMapping("/{ticketTypeId}")
  public TicketTypeResponseDto getTicketType(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID eventId,
      @PathVariable UUID ticketTypeId) {
    return ticketTypeService.getTicketType(parseUserId(jwt), eventId, ticketTypeId)
        .map(ticketTypeMapper::toResponseDto)
        .orElseThrow(() -> new TicketTypeNotFoundException(
            "Ticket type with id '%s' was not found".formatted(ticketTypeId)));
  }

  @PutMapping("/{ticketTypeId}")
  public TicketTypeResponseDto updateTicketType(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID eventId,
      @PathVariable UUID ticketTypeId,
      @Valid @RequestBody TicketTypeRequestDto request) {
    TicketType ticketType = ticketTypeService.updateTicketType(
        parseUserId(jwt), eventId, ticketTypeId, request);
    return ticketTypeMapper.toResponseDto(ticketType);
  }

  @DeleteMapping("/{ticketTypeId}")
  public ResponseEntity<Void> deleteTicketType(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID eventId,
      @PathVariable UUID ticketTypeId) {
    ticketTypeService.deleteTicketType(parseUserId(jwt), eventId, ticketTypeId);
    return ResponseEntity.noContent().build();
  }
}
