package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.config.RazorpayProperties;
import com.showhop.api.entity.Event;
import com.showhop.api.entity.TicketReservation;
import com.showhop.api.entity.TicketType;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.EventStatus;
import com.showhop.api.entity.enums.ReservationState;
import com.showhop.api.repository.EventRepository;
import com.showhop.api.repository.TicketReservationRepository;
import com.showhop.api.repository.TicketTypeRepository;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.service.impl.ReservationReaper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReservationReaperTest {

  @Autowired private TicketReservationRepository ticketReservationRepository;
  @Autowired private TicketTypeRepository ticketTypeRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private UserRepository userRepository;

  private ReservationReaper reaper;

  private void init() {
    reaper = new ReservationReaper(
        ticketReservationRepository,
        new RazorpayProperties(null, null, null, null, Duration.ofMinutes(12), 100));
  }

  @Test
  void expiresOnlyHeldReservationsPastTheirExpiry() {
    init();
    TicketType ticketType = aTicketType();
    TicketReservation expired = aReservation(
        ticketType, ReservationState.HELD, Instant.now().minus(1, ChronoUnit.MINUTES));
    TicketReservation notYetExpired = aReservation(
        ticketType, ReservationState.HELD, Instant.now().plus(10, ChronoUnit.MINUTES));
    TicketReservation alreadyConfirmed = aReservation(
        ticketType, ReservationState.CONFIRMED, Instant.now().minus(1, ChronoUnit.MINUTES));

    int count = reaper.expireDueReservations();

    assertThat(count).isEqualTo(1);
    assertThat(reload(expired).getState()).isEqualTo(ReservationState.EXPIRED);
    assertThat(reload(notYetExpired).getState()).isEqualTo(ReservationState.HELD);
    assertThat(reload(alreadyConfirmed).getState()).isEqualTo(ReservationState.CONFIRMED);
  }

  @Test
  void freedInventoryIsImmediatelyReflectedInActiveHoldsCount() {
    init();
    TicketType ticketType = aTicketType();
    aReservation(ticketType, ReservationState.HELD, Instant.now().minus(1, ChronoUnit.MINUTES));
    assertThat(ticketReservationRepository.countActiveHolds(ticketType.getId())).isZero();

    reaper.expireDueReservations();

    assertThat(ticketReservationRepository.countActiveHolds(ticketType.getId())).isZero();
  }

  private TicketReservation reload(TicketReservation reservation) {
    return ticketReservationRepository.findById(reservation.getId()).orElseThrow();
  }

  private TicketReservation aReservation(TicketType ticketType, ReservationState state, Instant expiresAt) {
    User buyer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Buyer").email("buyer-" + UUID.randomUUID() + "@example.com").build());
    return ticketReservationRepository.saveAndFlush(TicketReservation.builder()
        .ticketType(ticketType).buyer(buyer).quantity(1).state(state).expiresAt(expiresAt)
        .idempotencyKey("idem-" + UUID.randomUUID()).build());
  }

  private TicketType aTicketType() {
    User organizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Organizer").email("organizer-" + UUID.randomUUID() + "@example.com").build());
    Instant now = Instant.now();
    Event event = eventRepository.saveAndFlush(Event.builder()
        .name("Autumn Tech Meetup").venue("Riverside Hall")
        .startsAt(now.plusSeconds(3600)).endsAt(now.plusSeconds(7200))
        .status(EventStatus.PUBLISHED).organizer(organizer).build());
    return ticketTypeRepository.saveAndFlush(TicketType.builder()
        .event(event).name("General Admission").price(new BigDecimal("29.99")).totalAvailable(200).build());
  }
}
