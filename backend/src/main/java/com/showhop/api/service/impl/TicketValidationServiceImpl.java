package com.showhop.api.service.impl;

import com.showhop.api.entity.Ticket;
import com.showhop.api.entity.TicketValidation;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.TicketStatus;
import com.showhop.api.entity.enums.TicketValidationMethod;
import com.showhop.api.entity.enums.TicketValidationStatus;
import com.showhop.api.entity.enums.WebhookEventType;
import com.showhop.api.exception.TicketNotFoundException;
import com.showhop.api.exception.UserNotFoundException;
import com.showhop.api.repository.TicketRepository;
import com.showhop.api.repository.TicketValidationRepository;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.service.TicketValidationService;
import com.showhop.api.service.WebhookEventPublisher;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketValidationServiceImpl implements TicketValidationService {

  private final TicketRepository ticketRepository;
  private final TicketValidationRepository ticketValidationRepository;
  private final UserRepository userRepository;
  private final WebhookEventPublisher webhookEventPublisher;

  @Override
  @Transactional
  public TicketValidation validateTicket(
      UUID staffId, UUID ticketId, TicketValidationMethod method) {
    Ticket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new TicketNotFoundException(
            "Ticket with id '%s' was not found".formatted(ticketId)));

    User staff = userRepository.findById(staffId)
        .orElseThrow(() -> new UserNotFoundException(
            "User with id '%s' was not found".formatted(staffId)));

    TicketValidationStatus status = decideStatus(ticket);

    TicketValidation validation = TicketValidation.builder()
        .ticket(ticket)
        .validatedBy(staff)
        .status(status)
        .method(method)
        .validatedAt(Instant.now())
        .build();

    TicketValidation saved = ticketValidationRepository.save(validation);

    webhookEventPublisher.publish(
        ticket.getTicketType().getEvent().getOrganizer().getId(),
        WebhookEventType.TICKET_VALIDATED, Map.of(
            "ticketId", ticket.getId().toString(),
            "status", status.name(),
            "method", method.name()));

    return saved;
  }

  private TicketValidationStatus decideStatus(Ticket ticket) {
    if (ticket.getStatus() == TicketStatus.CANCELLED) {
      return TicketValidationStatus.INVALID;
    }
    boolean alreadyAdmitted = ticketValidationRepository
        .existsByTicketIdAndStatus(ticket.getId(), TicketValidationStatus.VALID);
    return alreadyAdmitted ? TicketValidationStatus.INVALID : TicketValidationStatus.VALID;
  }
}
