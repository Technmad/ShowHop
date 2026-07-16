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
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

/**
 * Runs full, realistically-shaped Razorpay payloads -- recorded fixtures
 * with the actual field set Razorpay's docs describe (entity, account_id,
 * contains, nested payment.entity with fee/tax/method/etc.), not the
 * minimal ad-hoc JSON the other webhook tests build inline. This is the
 * PRD's &sect;4.4 "Razorpay webhook fixture tests" item: it caught a real
 * bug during development -- the implementation originally read a
 * top-level "id" field for inbound-idempotency dedup, which these fixtures
 * (matching Razorpay's real schema) don't have, since Razorpay's classic
 * webhook payload carries no such field. Fixed in
 * RazorpayWebhookServiceImpl to dedupe on eventType+paymentId instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RazorpayWebhookFixtureTest {

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
  void theRecordedPaymentCapturedFixtureFulfillsTheReservation() throws Exception {
    TicketReservation reservation = seedHeldReservation();
    String rawBody = fixture("payment-captured.json", reservation.getRazorpayOrderId());

    postWebhook(rawBody).andExpect(status().isOk());

    TicketReservation reloaded = ticketReservationRepository.findById(reservation.getId()).orElseThrow();
    assertThat(reloaded.getState()).isEqualTo(ReservationState.CONFIRMED);
    assertThat(reloaded.getRazorpayPaymentId()).isEqualTo("pay_fixture_captured_001");
    assertThat(ticketRepository.countByTicketTypeIdAndStatus(
        reservation.getTicketType().getId(), TicketStatus.PURCHASED)).isEqualTo(1);
  }

  @Test
  void theRecordedPaymentFailedFixtureMarksTheReservationFailed() throws Exception {
    TicketReservation reservation = seedHeldReservation();
    String rawBody = fixture("payment-failed.json", reservation.getRazorpayOrderId());

    postWebhook(rawBody).andExpect(status().isOk());

    TicketReservation reloaded = ticketReservationRepository.findById(reservation.getId()).orElseThrow();
    assertThat(reloaded.getState()).isEqualTo(ReservationState.FAILED);
    assertThat(reloaded.getRazorpayPaymentId()).isEqualTo("pay_fixture_failed_001");
    assertThat(ticketRepository.countByTicketTypeIdAndStatus(
        reservation.getTicketType().getId(), TicketStatus.PURCHASED)).isZero();
  }

  private org.springframework.test.web.servlet.ResultActions postWebhook(String rawBody) throws Exception {
    String signature = webhookSigner.hmacHex(WEBHOOK_SECRET, rawBody);
    return mockMvc.perform(post("/api/v1/razorpay/webhook")
        .header("X-Razorpay-Signature", signature)
        .contentType(MediaType.APPLICATION_JSON)
        .content(rawBody));
  }

  private String fixture(String fileName, String razorpayOrderId) throws IOException {
    String raw = StreamUtils.copyToString(
        new ClassPathResource("razorpay/" + fileName).getInputStream(), StandardCharsets.UTF_8);
    return raw.replace("{{ORDER_ID}}", razorpayOrderId);
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
}
