package com.showhop.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.entity.Event;
import com.showhop.api.entity.TicketType;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.EventStatus;
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
class TicketTypeRepositoryTest {

  @Autowired
  private TicketTypeRepository ticketTypeRepository;

  @Autowired
  private EventRepository eventRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void savesATicketTypeWithAMonetaryPriceAndCapacity() {
    User organizer = userRepository.saveAndFlush(
        User.builder().id(UUID.randomUUID()).name("Priya").email("priya@example.com").build());

    Instant now = Instant.now();
    Event event = eventRepository.saveAndFlush(Event.builder()
        .name("Autumn Tech Meetup")
        .venue("Riverside Hall")
        .startsAt(now.plus(30, ChronoUnit.DAYS))
        .endsAt(now.plus(30, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS))
        .status(EventStatus.DRAFT)
        .organizer(organizer)
        .build());

    TicketType ticketType = TicketType.builder()
        .event(event)
        .name("General Admission")
        .price(new BigDecimal("29.99"))
        .totalAvailable(200)
        .build();

    TicketType saved = ticketTypeRepository.saveAndFlush(ticketType);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getPrice()).isEqualByComparingTo("29.99");
    assertThat(saved.getEvent().getId()).isEqualTo(event.getId());
  }
}
