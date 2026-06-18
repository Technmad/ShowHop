package com.showhop.api.web;

import static com.showhop.api.security.JwtUtil.parseUserId;

import com.showhop.api.dto.TicketValidationRequestDto;
import com.showhop.api.dto.TicketValidationResponseDto;
import com.showhop.api.entity.TicketValidation;
import com.showhop.api.mapper.TicketValidationMapper;
import com.showhop.api.service.TicketValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ticket-validations")
@RequiredArgsConstructor
public class TicketValidationController {

  private final TicketValidationService ticketValidationService;
  private final TicketValidationMapper ticketValidationMapper;

  @PostMapping
  public TicketValidationResponseDto validateTicket(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TicketValidationRequestDto request) {
    TicketValidation validation = ticketValidationService.validateTicket(
        parseUserId(jwt), request.ticketId(), request.method());
    return ticketValidationMapper.toResponseDto(validation);
  }
}
