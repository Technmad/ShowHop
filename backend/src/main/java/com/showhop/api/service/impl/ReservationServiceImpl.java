package com.showhop.api.service.impl;

import com.showhop.api.config.RazorpayProperties;
import com.showhop.api.entity.TicketReservation;
import com.showhop.api.entity.TicketType;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.EventStatus;
import com.showhop.api.entity.enums.ReservationState;
import com.showhop.api.entity.enums.TicketStatus;
import com.showhop.api.exception.EventNotFoundException;
import com.showhop.api.exception.RazorpayIntegrationException;
import com.showhop.api.exception.TicketReservationNotFoundException;
import com.showhop.api.exception.TicketTypeNotFoundException;
import com.showhop.api.exception.TicketsSoldOutException;
import com.showhop.api.exception.UserNotFoundException;
import com.showhop.api.repository.TicketRepository;
import com.showhop.api.repository.TicketReservationRepository;
import com.showhop.api.repository.TicketTypeRepository;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.service.ReservationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

  private final TicketTypeRepository ticketTypeRepository;
  private final TicketRepository ticketRepository;
  private final TicketReservationRepository ticketReservationRepository;
  private final UserRepository userRepository;
  private final RazorpayOrderClient razorpayOrderClient;
  private final RazorpayProperties razorpayProperties;

  @Override
  @Transactional
  public ReservationInitiationResult reserve(
      UUID buyerId, UUID eventId, UUID ticketTypeId, int quantity, String idempotencyKey) {
    User buyer = userRepository.findById(buyerId)
        .orElseThrow(() -> new UserNotFoundException(
            "User with id '%s' was not found".formatted(buyerId)));

    // Locked for the rest of this transaction, same discipline
    // TicketPurchaseServiceImpl already validates. A retried request
    // carrying the same idempotencyKey against the same ticket type
    // serializes on this lock too, so the idempotency check right below
    // is race-free without needing to catch a unique-constraint violation.
    TicketType ticketType = ticketTypeRepository.findByIdWithLock(ticketTypeId)
        .orElseThrow(() -> new TicketTypeNotFoundException(
            "Ticket type with id '%s' was not found".formatted(ticketTypeId)));

    var existing = ticketReservationRepository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
      return new ReservationInitiationResult(existing.get(), amountInPaise(ticketType, quantity));
    }

    if (!ticketType.getEvent().getId().equals(eventId)
        || ticketType.getEvent().getStatus() != EventStatus.PUBLISHED) {
      throw new EventNotFoundException(
          "Published event with id '%s' was not found".formatted(eventId));
    }

    int sold = ticketRepository.countByTicketTypeIdAndStatus(ticketTypeId, TicketStatus.PURCHASED);
    int activeHolds = ticketReservationRepository.countActiveHolds(ticketTypeId);
    int available = ticketType.getTotalAvailable() - sold - activeHolds;
    if (quantity > available) {
      throw new TicketsSoldOutException(
          "Ticket type '%s' is sold out".formatted(ticketTypeId));
    }

    TicketReservation reservation = ticketReservationRepository.save(TicketReservation.builder()
        .ticketType(ticketType)
        .buyer(buyer)
        .quantity(quantity)
        .state(ReservationState.HELD)
        .expiresAt(Instant.now().plus(razorpayProperties.reservationTtl()))
        .idempotencyKey(idempotencyKey)
        .build());

    long amountInPaise = amountInPaise(ticketType, quantity);
    RazorpayOrderClient.RazorpayOrder order;
    try {
      order = razorpayOrderClient.createOrder(amountInPaise, reservation.getId().toString());
    } catch (RestClientException e) {
      // Unchecked on purpose: propagating this rolls back the whole
      // transaction, including the reservation insert above, so a failed
      // Order creation (bad credentials, network blip, Razorpay outage)
      // never leaves an orphaned HELD row with no razorpayOrderId behind.
      throw new RazorpayIntegrationException(
          "Couldn't create the Razorpay order for reservation '%s'".formatted(reservation.getId()), e);
    }
    reservation.setRazorpayOrderId(order.id());

    return new ReservationInitiationResult(reservation, amountInPaise);
  }

  @Override
  @Transactional(readOnly = true)
  public TicketReservation getForBuyer(UUID buyerId, UUID reservationId) {
    return ticketReservationRepository.findByIdAndBuyerId(reservationId, buyerId)
        .orElseThrow(() -> new TicketReservationNotFoundException(
            "Reservation with id '%s' was not found".formatted(reservationId)));
  }

  private long amountInPaise(TicketType ticketType, int quantity) {
    return ticketType.getPrice()
        .multiply(BigDecimal.valueOf(100))
        .multiply(BigDecimal.valueOf(quantity))
        .longValueExact();
  }
}
