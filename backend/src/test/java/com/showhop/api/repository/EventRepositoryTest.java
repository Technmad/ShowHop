package com.showhop.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.entity.Event;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.EventStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
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

  @Test
  void searchPublishedOnlyMatchesPublishedEventsByNameOrVenue() {
    User organizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Priya").email("priya@example.com").build());
    Instant now = Instant.now();

    eventRepository.saveAndFlush(anEvent("Autumn Tech Meetup", "Riverside Hall",
        EventStatus.PUBLISHED, organizer, now));
    eventRepository.saveAndFlush(anEvent("Spring Jazz Night", "Autumn Gardens",
        EventStatus.PUBLISHED, organizer, now));
    eventRepository.saveAndFlush(anEvent("Autumn Food Fair", "Market Square",
        EventStatus.DRAFT, organizer, now)); // not published -- must not match

    var results = eventRepository.searchPublished("autumn", PageRequest.of(0, 10));

    assertThat(results.getContent())
        .extracting(Event::getName)
        .containsExactlyInAnyOrder("Autumn Tech Meetup", "Spring Jazz Night");
  }

  private Event anEvent(
      String name, String venue, EventStatus status, User organizer, Instant now) {
    return Event.builder()
        .name(name)
        .venue(venue)
        .startsAt(now.plus(30, ChronoUnit.DAYS))
        .endsAt(now.plus(30, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS))
        .status(status)
        .organizer(organizer)
        .build();
  }
}
