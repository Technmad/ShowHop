package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.showhop.api.config.RazorpayProperties;
import com.showhop.api.entity.Event;
import com.showhop.api.entity.TicketReservation;
import com.showhop.api.entity.TicketType;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.EventStatus;
import com.showhop.api.entity.enums.ReservationState;
import com.showhop.api.entity.enums.TicketStatus;
import com.showhop.api.exception.EventNotFoundException;
import com.showhop.api.exception.TicketReservationNotFoundException;
import com.showhop.api.exception.TicketsSoldOutException;
import com.showhop.api.repository.TicketRepository;
import com.showhop.api.repository.TicketReservationRepository;
import com.showhop.api.repository.TicketTypeRepository;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.service.ReservationService.ReservationInitiationResult;
import com.showhop.api.service.impl.RazorpayOrderClient;
import com.showhop.api.service.impl.ReservationServiceImpl;
import java.math.BigDecimal;
import java.time.Duration;
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
 * RazorpayOrderClient is deliberately a real instance backed by
 * MockRestServiceServer, not a Mockito mock -- mocking that concrete class
 * fails under this environment's JDK/Mockito combination
 * (MockitoException: "cannot mock this class"), and this technique is
 * already established in RazorpayOrderClientTest.
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

  @Mock
  private TicketTypeRepository ticketTypeRepository;
  @Mock
  private TicketRepository ticketRepository;
  @Mock
  private TicketReservationRepository ticketReservationRepository;
  @Mock
  private UserRepository userRepository;

  private MockRestServiceServer mockServer;
  private ReservationServiceImpl reservationService;

  @BeforeEach
  void setUp() {
    RazorpayProperties razorpayProperties = new RazorpayProperties(
        "rzp_test_key", "rzp_test_secret", "whsec_test", "https://api.razorpay.com/v1", Duration.ofMinutes(12), 100);
    RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    RazorpayOrderClient razorpayOrderClient = new RazorpayOrderClient(builder.build(), razorpayProperties);
    reservationService = new ReservationServiceImpl(
        ticketTypeRepository, ticketRepository, ticketReservationRepository, userRepository,
        razorpayOrderClient, razorpayProperties);
  }

  @Test
  void holdsInventoryAndCreatesARazorpayOrderWhenCapacityRemains() {
    UUID buyerId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    UUID ticketTypeId = UUID.randomUUID();
    User buyer = User.builder().id(buyerId).build();
    TicketType ticketType = aPublishedTicketType(eventId, ticketTypeId, new BigDecimal("299.00"), 10);

    when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
    when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(ticketType));
    when(ticketReservationRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
    when(ticketRepository.countByTicketTypeIdAndStatus(ticketTypeId, TicketStatus.PURCHASED))
        .thenReturn(5);
    when(ticketReservationRepository.countActiveHolds(ticketTypeId)).thenReturn(2);
    when(ticketReservationRepository.save(any(TicketReservation.class)))
        .thenAnswer(invocation -> {
          TicketReservation saved = invocation.getArgument(0);
          saved.setId(UUID.randomUUID());
          return saved;
        });
    mockServer.expect(requestTo("https://api.razorpay.com/v1/orders"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("""
            {"id":"order_abc123","amount":29900,"currency":"INR","receipt":"receipt"}
            """, MediaType.APPLICATION_JSON));

    ReservationInitiationResult result =
        reservationService.reserve(buyerId, eventId, ticketTypeId, 1, "idem-1");

    mockServer.verify();
    assertThat(result.reservation().getState()).isEqualTo(ReservationState.HELD);
    assertThat(result.reservation().getRazorpayOrderId()).isEqualTo("order_abc123");
    assertThat(result.amountInPaise()).isEqualTo(29900);
  }

  @Test
  void aRetriedRequestWithTheSameIdempotencyKeyReturnsTheExistingReservationWithoutCallingRazorpay() {
    UUID buyerId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    UUID ticketTypeId = UUID.randomUUID();
    TicketType ticketType = aPublishedTicketType(eventId, ticketTypeId, new BigDecimal("299.00"), 10);
    TicketReservation existingReservation = TicketReservation.builder()
        .id(UUID.randomUUID()).ticketType(ticketType).state(ReservationState.HELD)
        .idempotencyKey("idem-2").razorpayOrderId("order_existing").build();

    when(userRepository.findById(buyerId)).thenReturn(Optional.of(User.builder().id(buyerId).build()));
    when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(ticketType));
    when(ticketReservationRepository.findByIdempotencyKey("idem-2"))
        .thenReturn(Optional.of(existingReservation));

    ReservationInitiationResult result =
        reservationService.reserve(buyerId, eventId, ticketTypeId, 1, "idem-2");

    assertThat(result.reservation()).isSameAs(existingReservation);
    mockServer.verify(); // no expectation was set up, so any call to Razorpay here fails the test
  }

  @Test
  void rejectsAReservationThatWouldExceedAvailableInventoryAccountingForActiveHolds() {
    UUID buyerId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    UUID ticketTypeId = UUID.randomUUID();
    TicketType ticketType = aPublishedTicketType(eventId, ticketTypeId, new BigDecimal("299.00"), 10);

    when(userRepository.findById(buyerId)).thenReturn(Optional.of(User.builder().id(buyerId).build()));
    when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(ticketType));
    when(ticketReservationRepository.findByIdempotencyKey("idem-3")).thenReturn(Optional.empty());
    when(ticketRepository.countByTicketTypeIdAndStatus(ticketTypeId, TicketStatus.PURCHASED))
        .thenReturn(8);
    when(ticketReservationRepository.countActiveHolds(ticketTypeId)).thenReturn(2);

    assertThatThrownBy(() -> reservationService.reserve(buyerId, eventId, ticketTypeId, 1, "idem-3"))
        .isInstanceOf(TicketsSoldOutException.class);
    mockServer.verify(); // no expectation was set up, so any call to Razorpay here fails the test
  }

  @Test
  void rejectsAReservationForATicketTypeThatDoesNotBelongToThatEvent() {
    UUID buyerId = UUID.randomUUID();
    UUID wrongEventId = UUID.randomUUID();
    UUID ticketTypeId = UUID.randomUUID();
    TicketType ticketType = aPublishedTicketType(UUID.randomUUID(), ticketTypeId, new BigDecimal("299.00"), 10);

    when(userRepository.findById(buyerId)).thenReturn(Optional.of(User.builder().id(buyerId).build()));
    when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(ticketType));
    when(ticketReservationRepository.findByIdempotencyKey("idem-4")).thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> reservationService.reserve(buyerId, wrongEventId, ticketTypeId, 1, "idem-4"))
        .isInstanceOf(EventNotFoundException.class);
  }

  @Test
  void getForBuyerThrowsWhenTheReservationDoesNotBelongToThatBuyer() {
    UUID buyerId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();

    when(ticketReservationRepository.findByIdAndBuyerId(reservationId, buyerId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> reservationService.getForBuyer(buyerId, reservationId))
        .isInstanceOf(TicketReservationNotFoundException.class);
  }

  private TicketType aPublishedTicketType(
      UUID eventId, UUID ticketTypeId, BigDecimal price, int totalAvailable) {
    User organizer = User.builder().id(UUID.randomUUID()).build();
    Event event = Event.builder().id(eventId).status(EventStatus.PUBLISHED).organizer(organizer).build();
    return TicketType.builder()
        .id(ticketTypeId)
        .event(event)
        .price(price)
        .totalAvailable(totalAvailable)
        .build();
  }
}
