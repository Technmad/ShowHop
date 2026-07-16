package com.showhop.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.showhop.api.entity.ProcessedRazorpayEvent;
import com.showhop.api.entity.Ticket;
import com.showhop.api.entity.TicketReservation;
import com.showhop.api.entity.TicketType;
import com.showhop.api.entity.enums.ReservationState;
import com.showhop.api.entity.enums.TicketStatus;
import com.showhop.api.entity.enums.WebhookEventType;
import com.showhop.api.repository.ProcessedRazorpayEventRepository;
import com.showhop.api.repository.TicketRepository;
import com.showhop.api.repository.TicketReservationRepository;
import com.showhop.api.repository.TicketTypeRepository;
import com.showhop.api.service.QrCodeService;
import com.showhop.api.service.RazorpayWebhookService;
import com.showhop.api.service.WebhookEventPublisher;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fulfills reservations from real Razorpay confirmation (PRD &sect;4.2) --
 * this, not the client-side Checkout callback, is the only trusted signal.
 * A reservation whose Razorpay order isn't recognized (no matching
 * {@code razorpayOrderId}) is logged and dropped rather than thrown as a
 * 404: an unrecognized order is either a stray test-mode event or a race
 * with the reservation's own insert, and returning 2xx avoids Razorpay
 * retrying an event this app will never be able to match.
 */
@Service
@RequiredArgsConstructor
public class RazorpayWebhookServiceImpl implements RazorpayWebhookService {

  private static final String PAYMENT_CAPTURED = "payment.captured";
  private static final String PAYMENT_FAILED = "payment.failed";

  private final ObjectMapper objectMapper;
  private final ProcessedRazorpayEventRepository processedRazorpayEventRepository;
  private final TicketReservationRepository ticketReservationRepository;
  private final TicketTypeRepository ticketTypeRepository;
  private final TicketRepository ticketRepository;
  private final QrCodeService qrCodeService;
  private final WebhookEventPublisher webhookEventPublisher;
  private final RazorpayRefundClient razorpayRefundClient;

  @Override
  @Transactional
  public void handle(String rawBody) {
    JsonNode event = parse(rawBody);

    String eventType = event.path("event").asText("");
    JsonNode paymentEntity = event.path("payload").path("payment").path("entity");
    String razorpayOrderId = paymentEntity.path("order_id").asText(null);
    String razorpayPaymentId = paymentEntity.path("id").asText(null);
    if (razorpayPaymentId == null) {
      return;
    }

    // Razorpay's classic webhook payload carries no top-level, stable
    // event id (unlike Stripe) -- the dedupe key is synthesized from the
    // event type plus the payment id instead, which is stable and unique
    // per payment. Still the same inbound-idempotency discipline as the
    // rest of the saga (PRD 4.2): a redelivered payment.captured for the
    // same payment must be a no-op.
    String dedupeKey = eventType + ":" + razorpayPaymentId;
    if (processedRazorpayEventRepository.existsById(dedupeKey)) {
      return;
    }
    processedRazorpayEventRepository.save(new ProcessedRazorpayEvent(dedupeKey, Instant.now()));

    if (razorpayOrderId == null) {
      return;
    }

    switch (eventType) {
      case PAYMENT_CAPTURED -> handlePaymentCaptured(razorpayOrderId, razorpayPaymentId);
      case PAYMENT_FAILED -> handlePaymentFailed(razorpayOrderId, razorpayPaymentId);
      default -> { /* not an event this app fulfills or compensates on */ }
    }
  }

  private void handlePaymentCaptured(String razorpayOrderId, String razorpayPaymentId) {
    ticketReservationRepository.findByRazorpayOrderIdWithLock(razorpayOrderId).ifPresent(reservation -> {
      reservation.setRazorpayPaymentId(razorpayPaymentId);
      if (reservation.getState() != ReservationState.HELD) {
        return; // already CONFIRMED (redelivery) or terminal -- nothing to (re)do
      }

      if (reservation.getExpiresAt().isBefore(Instant.now()) && !capacityStillAvailable(reservation)) {
        // Paid but expired, and capacity is now gone: the compensation
        // path (PRD 4.2, 7) -- never silently drop a real payment.
        razorpayRefundClient.refundFull(razorpayPaymentId);
        reservation.setState(ReservationState.FAILED);
        return;
      }

      fulfil(reservation);
    });
  }

  private boolean capacityStillAvailable(TicketReservation reservation) {
    // Re-lock: the hold no longer counts toward activeHolds once expired
    // (countActiveHolds excludes it), so a concurrent purchase/reservation
    // may have taken the capacity this hold used to occupy.
    TicketType ticketType = ticketTypeRepository.findByIdWithLock(reservation.getTicketType().getId())
        .orElseThrow();
    int sold = ticketRepository.countByTicketTypeIdAndStatus(ticketType.getId(), TicketStatus.PURCHASED);
    return ticketType.getTotalAvailable() - sold >= reservation.getQuantity();
  }

  private void fulfil(TicketReservation reservation) {
    reservation.setState(ReservationState.CONFIRMED);
    TicketType ticketType = reservation.getTicketType();

    for (int i = 0; i < reservation.getQuantity(); i++) {
      Ticket ticket = ticketRepository.save(Ticket.builder()
          .ticketType(ticketType)
          .purchaser(reservation.getBuyer())
          .status(TicketStatus.PURCHASED)
          .build());
      qrCodeService.generateQrCode(ticket);

      webhookEventPublisher.publish(
          ticketType.getEvent().getOrganizer().getId(), WebhookEventType.TICKET_PURCHASED, Map.of(
              "ticketId", ticket.getId().toString(),
              "ticketTypeId", ticketType.getId().toString(),
              "eventId", ticketType.getEvent().getId().toString(),
              "purchaserId", reservation.getBuyer().getId().toString()));
    }
  }

  private void handlePaymentFailed(String razorpayOrderId, String razorpayPaymentId) {
    ticketReservationRepository.findByRazorpayOrderIdWithLock(razorpayOrderId).ifPresent(reservation -> {
      reservation.setRazorpayPaymentId(razorpayPaymentId);
      if (reservation.getState() == ReservationState.HELD) {
        reservation.setState(ReservationState.FAILED);
      }
    });
  }

  private JsonNode parse(String rawBody) {
    try {
      return objectMapper.readTree(rawBody);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Malformed Razorpay webhook payload", e);
    }
  }
}
