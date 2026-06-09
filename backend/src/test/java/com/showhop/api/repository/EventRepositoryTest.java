package com.showhop.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.entity.Event;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.EventStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventRepositoryTest {

  @Autowired
  private EventRepository eventRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void savesAnEventOwnedByAnOrganizer() {
    User organizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID())
        .name("Priya Shah")
        .email("priya@example.com")
        .build());

    Instant now = Instant.now();
    Event event = Event.builder()
        .name("Autumn Tech Meetup")
        .venue("Riverside Hall")
        .startsAt(now.plus(30, ChronoUnit.DAYS))
        .endsAt(now.plus(30, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS))
        .status(EventStatus.DRAFT)
        .organizer(organizer)
        .build();

    Event saved = eventRepository.saveAndFlush(event);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getStatus()).isEqualTo(EventStatus.DRAFT);
    assertThat(saved.getOrganizer().getId()).isEqualTo(organizer.getId());
  }
}
