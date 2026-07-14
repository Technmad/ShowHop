package com.showhop.api.web;

import static com.showhop.api.security.JwtUtil.parseUserId;

import com.showhop.api.config.RazorpayProperties;
import com.showhop.api.dto.ReservationInitiationResponseDto;
import com.showhop.api.dto.ReservationRequestDto;
import com.showhop.api.mapper.ReservationMapper;
import com.showhop.api.service.ReservationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lives under /published-events, not /events -- deliberately off the
 * ORGANIZER-only matcher (see SecurityConfig), same reasoning as
 * TicketPurchaseController: any authenticated role can reserve a ticket.
 * The PRD's API-surface table (&sect;4.2) originally sketched this under
 * /events/{eventId}/ticket-types/{ticketTypeId}/reservations; moved here
 * during implementation once that collision with the existing
 * ORGANIZER-only matcher on /api/v1/events/** surfaced.
 */
@RestController
@RequestMapping("/api/v1/published-events/{eventId}/ticket-types/{ticketTypeId}/reservations")
@RequiredArgsConstructor
public class ReservationController {

  private final ReservationService reservationService;
  private final ReservationMapper reservationMapper;
  private final RazorpayProperties razorpayProperties;

  @PostMapping
  public ResponseEntity<ReservationInitiationResponseDto> reserve(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID eventId,
      @PathVariable UUID ticketTypeId,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody ReservationRequestDto request) {
    var result = reservationService.reserve(
        parseUserId(jwt), eventId, ticketTypeId, request.quantity(), idempotencyKey);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(reservationMapper.toInitiationResponseDto(result, razorpayProperties.keyId()));
  }
}
