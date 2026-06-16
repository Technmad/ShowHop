package com.showhop.api.web;

import static com.showhop.api.security.JwtUtil.parseUserId;

import com.showhop.api.dto.TicketResponseDto;
import com.showhop.api.entity.Ticket;
import com.showhop.api.mapper.TicketMapper;
import com.showhop.api.service.TicketPurchaseService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Purchasing lives under /published-events, not /events -- deliberately off
 * the ORGANIZER-only matcher, since any authenticated role can buy a
 * ticket. See SecurityConfig for the matcher this depends on.
 */
@RestController
@RequestMapping("/api/v1/published-events/{eventId}/ticket-types/{ticketTypeId}/tickets")
@RequiredArgsConstructor
public class TicketPurchaseController {

  private final TicketPurchaseService ticketPurchaseService;
  private final TicketMapper ticketMapper;

  @PostMapping
  public ResponseEntity<TicketResponseDto> purchaseTicket(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID eventId,
      @PathVariable UUID ticketTypeId) {
    Ticket ticket = ticketPurchaseService.purchaseTicket(parseUserId(jwt), eventId, ticketTypeId);
    return ResponseEntity.status(HttpStatus.CREATED).body(ticketMapper.toResponseDto(ticket));
  }
}
