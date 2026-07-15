package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showhop.api.config.RazorpayProperties;
import com.showhop.api.entity.Event;
import com.showhop.api.entity.QrCode;
import com.showhop.api.entity.Ticket;
import com.showhop.api.entity.TicketReservation;
import com.showhop.api.entity.TicketType;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.EventStatus;
import com.showhop.api.entity.enums.ReservationState;
import com.showhop.api.entity.enums.TicketStatus;
import com.showhop.api.entity.enums.WebhookEventType;
import com.showhop.api.repository.ProcessedRazorpayEventRepository;
import com.showhop.api.repository.TicketRepository;
import com.showhop.api.repository.TicketReservationRepository;
import com.showhop.api.repository.TicketTypeRepository;
import com.showhop.api.service.impl.RazorpayRefundClient;
import com.showhop.api.service.impl.RazorpayWebhookServiceImpl;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * RazorpayRefundClient is a real instance backed by MockRestServiceServer,
 * not a Mockito mock -- same reasoning as ReservationServiceImplTest:
 * mocking that concrete class fails under this environment's JDK/Mockito
 * combination.
 */
@ExtendWith(MockitoExtension.class)
class RazorpayWebhookServiceImplTest {

  @Mock
  private ProcessedRazorpayEventRepository processedRazorpayEventRepository;
  @Mock
  private TicketReservationRepository ticketReservationRepository;
  @Mock
  private TicketTypeRepository ticketTypeRepository;
  @Mock
  private TicketRepository ticketRepository;
  @Mock
  private QrCodeService qrCodeService;
  @Mock
  private WebhookEventPublisher webhookEventPublisher;

  private MockRestServiceServer mockServer;
  private RazorpayWebhookServiceImpl webhookService;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    RazorpayProperties properties = new RazorpayProperties(
        "rzp_test_key", "rzp_test_secret", "whsec_test", "https://api.razorpay.com/v1", Duration.ofMinutes(12));
    RazorpayRefundClient razorpayRefundClient = new RazorpayRefundClient(builder.build(), properties);

    webhookService = new RazorpayWebhookServiceImpl(
        new ObjectMapper(), processedRazorpayEventRepository, ticketReservationRepository,
        ticketTypeRepository, ticketRepository, qrCodeService, webhookEventPublisher, razorpayRefundClient);
  }

  @Test
  void paymentCapturedFulfillsAHeldReservationAndPublishesTicketPurchased() {
    TicketReservation reservation = aReservation(
        ReservationState.HELD, Instant.now().plus(10, ChronoUnit.MINUTES), 1);
    when(processedRazorpayEventRepository.existsById("evt_1")).thenReturn(false);
    when(ticketReservationRepository.findByRazorpayOrderIdWithLock("order_abc"))
        .thenReturn(Optional.of(reservation));
    when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
      Ticket ticket = invocation.getArgument(0);
      ticket.setId(UUID.randomUUID());
      return ticket;
    });
    when(qrCodeService.generateQrCode(any(Ticket.class))).thenReturn(new QrCode());

    webhookService.handle(paymentEvent("evt_1", "payment.captured", "order_abc", "pay_xyz"));

    assertThat(reservation.getState()).isEqualTo(ReservationState.CONFIRMED);
    assertThat(reservation.getRazorpayPaymentId()).isEqualTo("pay_xyz");
    verify(ticketRepository, times(1)).save(any(Ticket.class));
    verify(webhookEventPublisher).publish(
        eq(reservation.getTicketType().getEvent().getOrganizer().getId()),
        eq(WebhookEventType.TICKET_PURCHASED), any());
    verify(processedRazorpayEventRepository).save(any());
  }

  @Test
  void aRedeliveredEventIsANoOp() {
    when(processedRazorpayEventRepository.existsById("evt_1")).thenReturn(true);

    webhookService.handle(paymentEvent("evt_1", "payment.captured", "order_abc", "pay_xyz"));

    verifyNoInteractions(ticketReservationRepository, ticketRepository, qrCodeService, webhookEventPublisher);
    verify(processedRazorpayEventRepository, never()).save(any());
  }

  @Test
  void paymentFailedMarksAHeldReservationFailed() {
    TicketReservation reservation = aReservation(
        ReservationState.HELD, Instant.now().plus(10, ChronoUnit.MINUTES), 1);
    when(processedRazorpayEventRepository.existsById("evt_2")).thenReturn(false);
    when(ticketReservationRepository.findByRazorpayOrderIdWithLock("order_abc"))
        .thenReturn(Optional.of(reservation));

    webhookService.handle(paymentEvent("evt_2", "payment.failed", "order_abc", "pay_xyz"));

    assertThat(reservation.getState()).isEqualTo(ReservationState.FAILED);
    verifyNoInteractions(ticketRepository, qrCodeService, webhookEventPublisher);
  }

  @Test
  void paidButExpiredWithNoCapacityLeftTriggersAnAutoRefundInsteadOfFulfilling() {
    TicketReservation reservation = aReservation(
        ReservationState.HELD, Instant.now().minus(1, ChronoUnit.MINUTES), 1);
    reservation.getTicketType().setTotalAvailable(5);
    when(processedRazorpayEventRepository.existsById("evt_3")).thenReturn(false);
    when(ticketReservationRepository.findByRazorpayOrderIdWithLock("order_abc"))
        .thenReturn(Optional.of(reservation));
    when(ticketTypeRepository.findByIdWithLock(reservation.getTicketType().getId()))
        .thenReturn(Optional.of(reservation.getTicketType()));
    when(ticketRepository.countByTicketTypeIdAndStatus(reservation.getTicketType().getId(), TicketStatus.PURCHASED))
        .thenReturn(5); // fully sold -- no capacity left for the expired hold

    mockServer.expect(requestTo("https://api.razorpay.com/v1/payments/pay_xyz/refund"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("""
            {"id":"rfnd_1","status":"processed"}
            """, MediaType.APPLICATION_JSON));

    webhookService.handle(paymentEvent("evt_3", "payment.captured", "order_abc", "pay_xyz"));

    mockServer.verify();
    assertThat(reservation.getState()).isEqualTo(ReservationState.FAILED);
    verify(ticketRepository, never()).save(any());
    verifyNoInteractions(qrCodeService, webhookEventPublisher);
  }

  @Test
  void paidButExpiredWithCapacityStillAvailableFulfillsAnyway() {
    TicketReservation reservation = aReservation(
        ReservationState.HELD, Instant.now().minus(1, ChronoUnit.MINUTES), 1);
    reservation.getTicketType().setTotalAvailable(5);
    when(processedRazorpayEventRepository.existsById("evt_4")).thenReturn(false);
    when(ticketReservationRepository.findByRazorpayOrderIdWithLock("order_abc"))
        .thenReturn(Optional.of(reservation));
    when(ticketTypeRepository.findByIdWithLock(reservation.getTicketType().getId()))
        .thenReturn(Optional.of(reservation.getTicketType()));
    when(ticketRepository.countByTicketTypeIdAndStatus(reservation.getTicketType().getId(), TicketStatus.PURCHASED))
        .thenReturn(2); // capacity still available even though the hold expired
    when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
      Ticket ticket = invocation.getArgument(0);
      ticket.setId(UUID.randomUUID());
      return ticket;
    });
    when(qrCodeService.generateQrCode(any(Ticket.class))).thenReturn(new QrCode());

    webhookService.handle(paymentEvent("evt_4", "payment.captured", "order_abc", "pay_xyz"));

    assertThat(reservation.getState()).isEqualTo(ReservationState.CONFIRMED);
    verify(ticketRepository, times(1)).save(any(Ticket.class));
  }

  @Test
  void anUnrecognizedEventTypeIsIgnored() {
    when(processedRazorpayEventRepository.existsById("evt_5")).thenReturn(false);

    webhookService.handle(paymentEvent("evt_5", "payment.authorized", "order_abc", "pay_xyz"));

    verify(processedRazorpayEventRepository).save(any());
    verifyNoInteractions(ticketReservationRepository, ticketRepository, qrCodeService, webhookEventPublisher);
  }

  private TicketReservation aReservation(ReservationState state, Instant expiresAt, int quantity) {
    User organizer = User.builder().id(UUID.randomUUID()).build();
    Event event = Event.builder().id(UUID.randomUUID()).status(EventStatus.PUBLISHED).organizer(organizer).build();
    TicketType ticketType = TicketType.builder()
        .id(UUID.randomUUID()).event(event).totalAvailable(10).build();
    User buyer = User.builder().id(UUID.randomUUID()).build();
    return TicketReservation.builder()
        .id(UUID.randomUUID()).ticketType(ticketType).buyer(buyer).quantity(quantity)
        .state(state).expiresAt(expiresAt).idempotencyKey("idem-" + UUID.randomUUID())
        .razorpayOrderId("order_abc").build();
  }

  private String paymentEvent(String eventId, String eventType, String orderId, String paymentId) {
    return """
        {"id":"%s","event":"%s","payload":{"payment":{"entity":{"id":"%s","order_id":"%s"}}}}
        """.formatted(eventId, eventType, paymentId, orderId);
  }
}
