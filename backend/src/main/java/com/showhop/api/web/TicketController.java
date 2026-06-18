package com.showhop.api.web;

import static com.showhop.api.security.JwtUtil.parseUserId;

import com.showhop.api.dto.TicketResponseDto;
import com.showhop.api.exception.TicketNotFoundException;
import com.showhop.api.mapper.TicketMapper;
import com.showhop.api.service.QrCodeService;
import com.showhop.api.service.TicketService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

  private final TicketService ticketService;
  private final TicketMapper ticketMapper;
  private final QrCodeService qrCodeService;

  @GetMapping
  public Page<TicketResponseDto> listTickets(
      @AuthenticationPrincipal Jwt jwt, Pageable pageable) {
    return ticketService.listTicketsForUser(parseUserId(jwt), pageable)
        .map(ticketMapper::toResponseDto);
  }

  @GetMapping("/{ticketId}")
  public TicketResponseDto getTicket(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID ticketId) {
    return ticketService.getTicketForUser(parseUserId(jwt), ticketId)
        .map(ticketMapper::toResponseDto)
        .orElseThrow(() -> new TicketNotFoundException(
            "Ticket with id '%s' was not found".formatted(ticketId)));
  }

  @GetMapping("/{ticketId}/qr-codes")
  public ResponseEntity<byte[]> getTicketQrCode(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID ticketId) {
    byte[] image = qrCodeService.getQrCodeImageForUserAndTicket(parseUserId(jwt), ticketId);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.IMAGE_PNG);
    headers.setContentLength(image.length);

    return ResponseEntity.ok().headers(headers).body(image);
  }
}
