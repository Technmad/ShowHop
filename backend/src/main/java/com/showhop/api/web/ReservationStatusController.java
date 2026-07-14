package com.showhop.api.web;

import static com.showhop.api.security.JwtUtil.parseUserId;

import com.showhop.api.dto.ReservationStatusDto;
import com.showhop.api.mapper.ReservationMapper;
import com.showhop.api.service.ReservationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Buyer polls this while Checkout is open -- the webhook is what actually confirms it (PRD &sect;4.2). */
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationStatusController {

  private final ReservationService reservationService;
  private final ReservationMapper reservationMapper;

  @GetMapping("/{reservationId}")
  public ReservationStatusDto getReservation(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID reservationId) {
    var reservation = reservationService.getForBuyer(parseUserId(jwt), reservationId);
    return reservationMapper.toStatusDto(reservation);
  }
}
