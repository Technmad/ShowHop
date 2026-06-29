package com.showhop.api.service.impl;

import com.showhop.api.entity.Ticket;
import com.showhop.api.entity.TicketType;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.EventStatus;
import com.showhop.api.entity.enums.TicketStatus;
import com.showhop.api.entity.enums.WebhookEventType;
import com.showhop.api.exception.EventNotFoundException;
import com.showhop.api.exception.TicketTypeNotFoundException;
import com.showhop.api.exception.TicketsSoldOutException;
import com.showhop.api.exception.UserNotFoundException;
import com.showhop.api.repository.TicketRepository;
import com.showhop.api.repository.TicketTypeRepository;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.service.QrCodeService;
import com.showhop.api.service.TicketPurchaseService;
import com.showhop.api.service.WebhookEventPublisher;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketPurchaseServiceImpl implements TicketPurchaseService {

  private final TicketTypeRepository ticketTypeRepository;
  private final TicketRepository ticketRepository;
  private final UserRepository userRepository;
  private final QrCodeService qrCodeService;
  private final WebhookEventPublisher webhookEventPublisher;

  @Override
  @Transactional
  public Ticket purchaseTicket(UUID buyerId, UUID eventId, UUID ticketTypeId) {
    User buyer = userRepository.findById(buyerId)
        .orElseThrow(() -> new UserNotFoundException(
            "User with id '%s' was not found".formatted(buyerId)));

    // Locked for the rest of this transaction: a second concurrent
    // purchase of the same ticket type blocks here until this transaction
    // commits or rolls back, which is what makes the availability check
    // below race-free.
    TicketType ticketType = ticketTypeRepository.findByIdWithLock(ticketTypeId)
        .orElseThrow(() -> new TicketTypeNotFoundException(
            "Ticket type with id '%s' was not found".formatted(ticketTypeId)));

    if (!ticketType.getEvent().getId().equals(eventId)
        || ticketType.getEvent().getStatus() != EventStatus.PUBLISHED) {
      throw new EventNotFoundException(
          "Published event with id '%s' was not found".formatted(eventId));
    }

    int sold = ticketRepository.countByTicketTypeIdAndStatus(ticketTypeId, TicketStatus.PURCHASED);
    if (sold + 1 > ticketType.getTotalAvailable()) {
      throw new TicketsSoldOutException(
          "Ticket type '%s' is sold out".formatted(ticketTypeId));
    }

    Ticket ticket = Ticket.builder()
        .ticketType(ticketType)
        .purchaser(buyer)
        .status(TicketStatus.PURCHASED)
        .build();

    Ticket savedTicket = ticketRepository.save(ticket);
    qrCodeService.generateQrCode(savedTicket);

    webhookEventPublisher.publish(
        ticketType.getEvent().getOrganizer().getId(), WebhookEventType.TICKET_PURCHASED, Map.of(
            "ticketId", savedTicket.getId().toString(),
            "ticketTypeId", ticketTypeId.toString(),
            "eventId", eventId.toString(),
            "purchaserId", buyerId.toString()));

    return savedTicket;
  }
}
