package com.showhop.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.showhop.api.entity.Event;
import com.showhop.api.entity.TicketReservation;
import com.showhop.api.entity.TicketType;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.EventStatus;
import com.showhop.api.entity.enums.ReservationState;
import com.showhop.api.entity.enums.TicketStatus;
import com.showhop.api.repository.EventRepository;
import com.showhop.api.repository.TicketRepository;
import com.showhop.api.repository.TicketReservationRepository;
import com.showhop.api.repository.TicketTypeRepository;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.service.impl.WebhookSigner;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end through the real signature verifier and the real fulfillment
 * service against a real (H2) database -- this is the one place that
 * exercises "a genuine Razorpay webhook request turns a HELD reservation
 * into a PURCHASED Ticket" as a single scenario, rather than the pieces
 * tested separately elsewhere (RazorpaySignatureVerifierTest,
 * RazorpayWebhookServiceImplTest).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RazorpayWebhookControllerTest {

  private static final String WEBHOOK_SECRET = "whsec_test_fake_secret"; // matches test application.properties

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private EventRepository eventRepository;
  @Autowired
  private TicketTypeRepository ticketTypeRepository;
  @Autowired
  private TicketReservationRepository ticketReservationRepository;
  @Autowired
  private TicketRepository ticketRepository;
  @Autowired
  private WebhookSigner webhookSigner;

  @Test
  void aValidSignatureFulfillsTheReservationAndCreatesATicket() throws Exception {
    TicketReservation reservation = seedHeldReservation();
    String rawBody = paymentCapturedJson(UUID.randomUUID().toString(), reservation.getRazorpayOrderId());
    String signature = webhookSigner.hmacHex(WEBHOOK_SECRET, rawBody);

    mockMvc.perform(post("/api/v1/razorpay/webhook")
            .header("X-Razorpay-Signature", signature)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawBody))
        .andExpect(status().isOk());

    TicketReservation reloaded = ticketReservationRepository.findById(reservation.getId()).orElseThrow();
    assertThat(reloaded.getState()).isEqualTo(ReservationState.CONFIRMED);
    assertThat(ticketRepository.countByTicketTypeIdAndStatus(
        reservation.getTicketType().getId(), TicketStatus.PURCHASED)).isEqualTo(1);
  }

  @Test
  void anInvalidSignatureIsRejectedAndNothingIsFulfilled() throws Exception {
    TicketReservation reservation = seedHeldReservation();
    String rawBody = paymentCapturedJson(UUID.randomUUID().toString(), reservation.getRazorpayOrderId());

    mockMvc.perform(post("/api/v1/razorpay/webhook")
            .header("X-Razorpay-Signature", "0000deadbeef0000")
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawBody))
        .andExpect(status().isUnauthorized());

    TicketReservation reloaded = ticketReservationRepository.findById(reservation.getId()).orElseThrow();
    assertThat(reloaded.getState()).isEqualTo(ReservationState.HELD);
  }

  @Test
  void aRedeliveredEventFulfillsExactlyOnce() throws Exception {
    TicketReservation reservation = seedHeldReservation();
    String rawBody = paymentCapturedJson(UUID.randomUUID().toString(), reservation.getRazorpayOrderId());
    String signature = webhookSigner.hmacHex(WEBHOOK_SECRET, rawBody);

    mockMvc.perform(post("/api/v1/razorpay/webhook")
            .header("X-Razorpay-Signature", signature)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawBody))
        .andExpect(status().isOk());
    mockMvc.perform(post("/api/v1/razorpay/webhook")
            .header("X-Razorpay-Signature", signature)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawBody))
        .andExpect(status().isOk());

    assertThat(ticketRepository.countByTicketTypeIdAndStatus(
        reservation.getTicketType().getId(), TicketStatus.PURCHASED)).isEqualTo(1);
  }

  private TicketReservation seedHeldReservation() {
    User organizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Organizer")
        .email("organizer-" + UUID.randomUUID() + "@example.com").build());
    User buyer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Buyer")
        .email("buyer-" + UUID.randomUUID() + "@example.com").build());
    Instant now = Instant.now();
    Event event = eventRepository.saveAndFlush(Event.builder()
        .name("Autumn Tech Meetup").venue("Riverside Hall")
        .startsAt(now.plusSeconds(3600)).endsAt(now.plusSeconds(7200))
        .status(EventStatus.PUBLISHED).organizer(organizer).build());
    TicketType ticketType = ticketTypeRepository.saveAndFlush(TicketType.builder()
        .event(event).name("General Admission").price(new BigDecimal("299.00"))
        .totalAvailable(10).build());

    return ticketReservationRepository.saveAndFlush(TicketReservation.builder()
        .ticketType(ticketType).buyer(buyer).quantity(1)
        .state(ReservationState.HELD).expiresAt(now.plus(10, ChronoUnit.MINUTES))
        .idempotencyKey("idem-" + UUID.randomUUID())
        .razorpayOrderId("order_" + UUID.randomUUID())
        .build());
  }

  private String paymentCapturedJson(String eventId, String razorpayOrderId) {
    return """
        {"id":"evt_%s","event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_xyz","order_id":"%s"}}}}
        """.formatted(eventId, razorpayOrderId);
  }
}
