package com.showhop.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.entity.Event;
import com.showhop.api.entity.TicketReservation;
import com.showhop.api.entity.TicketType;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.EventStatus;
import com.showhop.api.entity.enums.ReservationState;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TicketReservationRepositoryTest {

  @Autowired
  private TicketReservationRepository ticketReservationRepository;

  @Autowired
  private TicketTypeRepository ticketTypeRepository;

  @Autowired
  private EventRepository eventRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void savesAHeldReservationAndFindsItByIdempotencyKey() {
    TicketType ticketType = aTicketType("resv-1@example.com");

    TicketReservation reservation = ticketReservationRepository.saveAndFlush(TicketReservation.builder()
        .ticketType(ticketType)
        .buyer(userRepository.saveAndFlush(User.builder()
            .id(UUID.randomUUID()).name("Buyer").email("buyer-resv-1@example.com").build()))
        .quantity(2)
        .state(ReservationState.HELD)
        .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
        .idempotencyKey("idem-key-1")
        .build());

    var found = ticketReservationRepository.findByIdempotencyKey("idem-key-1").orElseThrow();

    assertThat(found.getId()).isEqualTo(reservation.getId());
    assertThat(found.getState()).isEqualTo(ReservationState.HELD);
    assertThat(found.getQuantity()).isEqualTo(2);
    assertThat(found.getRazorpayOrderId()).isNull();
  }

  @Test
  void countsOnlyUnexpiredHeldReservationsAsActiveHolds() {
    TicketType ticketType = aTicketType("resv-2@example.com");

    saveReservation(ticketType, ReservationState.HELD, Instant.now().plus(10, ChronoUnit.MINUTES), "idem-key-2a");
    saveReservation(ticketType, ReservationState.HELD, Instant.now().minus(1, ChronoUnit.MINUTES), "idem-key-2b");
    saveReservation(ticketType, ReservationState.CONFIRMED, Instant.now().plus(10, ChronoUnit.MINUTES), "idem-key-2c");

    int activeHolds = ticketReservationRepository.countActiveHolds(ticketType.getId());

    assertThat(activeHolds).isEqualTo(1);
  }

  @Test
  void findsExpiredHeldReservationsForTheReaper() {
    TicketType ticketType = aTicketType("resv-3@example.com");

    TicketReservation expired = saveReservation(
        ticketType, ReservationState.HELD, Instant.now().minus(1, ChronoUnit.MINUTES), "idem-key-3a");
    saveReservation(ticketType, ReservationState.HELD, Instant.now().plus(10, ChronoUnit.MINUTES), "idem-key-3b");

    var claimable = ticketReservationRepository.findExpiredHeld(10);

    assertThat(claimable).extracting(TicketReservation::getId).containsExactly(expired.getId());
  }

  private TicketReservation saveReservation(
      TicketType ticketType, ReservationState state, Instant expiresAt, String idempotencyKey) {
    return ticketReservationRepository.saveAndFlush(TicketReservation.builder()
        .ticketType(ticketType)
        .buyer(userRepository.saveAndFlush(User.builder()
            .id(UUID.randomUUID()).name("Buyer").email("buyer-" + idempotencyKey + "@example.com").build()))
        .quantity(1)
        .state(state)
        .expiresAt(expiresAt)
        .idempotencyKey(idempotencyKey)
        .build());
  }

  private TicketType aTicketType(String organizerEmail) {
    User organizer = userRepository.saveAndFlush(
        User.builder().id(UUID.randomUUID()).name("Organizer").email(organizerEmail).build());

    Instant now = Instant.now();
    Event event = eventRepository.saveAndFlush(Event.builder()
        .name("Autumn Tech Meetup")
        .venue("Riverside Hall")
        .startsAt(now.plus(30, ChronoUnit.DAYS))
        .endsAt(now.plus(30, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS))
        .status(EventStatus.PUBLISHED)
        .organizer(organizer)
        .build());

    return ticketTypeRepository.saveAndFlush(TicketType.builder()
        .event(event)
        .name("General Admission")
        .price(new BigDecimal("29.99"))
        .totalAvailable(200)
        .build());
  }
}
