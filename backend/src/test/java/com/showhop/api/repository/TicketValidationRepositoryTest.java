package com.showhop.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.entity.Event;
import com.showhop.api.entity.Ticket;
import com.showhop.api.entity.TicketType;
import com.showhop.api.entity.TicketValidation;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.EventStatus;
import com.showhop.api.entity.enums.TicketStatus;
import com.showhop.api.entity.enums.TicketValidationMethod;
import com.showhop.api.entity.enums.TicketValidationStatus;
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
class TicketValidationRepositoryTest {

  @Autowired
  private TicketValidationRepository ticketValidationRepository;
  @Autowired
  private TicketRepository ticketRepository;
  @Autowired
  private TicketTypeRepository ticketTypeRepository;
  @Autowired
  private EventRepository eventRepository;
  @Autowired
  private UserRepository userRepository;

  @Test
  void savesAQrScanValidationForATicket() {
    User organizer = userRepository.saveAndFlush(
        User.builder().id(UUID.randomUUID()).name("Priya").email("priya@example.com").build());
    User attendee = userRepository.saveAndFlush(
        User.builder().id(UUID.randomUUID()).name("Alex").email("alex@example.com").build());
    User staff = userRepository.saveAndFlush(
        User.builder().id(UUID.randomUUID()).name("Jordan").email("jordan@example.com").build());

    Instant now = Instant.now();
    Event event = eventRepository.saveAndFlush(Event.builder()
        .name("Autumn Tech Meetup")
        .venue("Riverside Hall")
        .startsAt(now.plus(1, ChronoUnit.HOURS))
        .endsAt(now.plus(4, ChronoUnit.HOURS))
        .status(EventStatus.PUBLISHED)
        .organizer(organizer)
        .build());

    TicketType ticketType = ticketTypeRepository.saveAndFlush(TicketType.builder()
        .event(event)
        .name("General Admission")
        .price(new BigDecimal("29.99"))
        .totalAvailable(200)
        .build());

    Ticket ticket = ticketRepository.saveAndFlush(Ticket.builder()
        .ticketType(ticketType)
        .purchaser(attendee)
        .status(TicketStatus.PURCHASED)
        .build());

    TicketValidation validation = TicketValidation.builder()
        .ticket(ticket)
        .validatedBy(staff)
        .status(TicketValidationStatus.VALID)
        .method(TicketValidationMethod.QR_SCAN)
        .validatedAt(now)
        .build();

    TicketValidation saved = ticketValidationRepository.saveAndFlush(validation);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getStatus()).isEqualTo(TicketValidationStatus.VALID);
    assertThat(saved.getMethod()).isEqualTo(TicketValidationMethod.QR_SCAN);
    assertThat(saved.getValidatedBy().getId()).isEqualTo(staff.getId());
  }
}
