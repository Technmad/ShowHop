package com.showhop.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.entity.User;
import com.showhop.api.entity.WebhookEvent;
import com.showhop.api.entity.enums.WebhookEventType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WebhookEventRepositoryTest {

  @Autowired
  private WebhookEventRepository webhookEventRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void savesAJsonPayloadAndFindsUnfannedOutEventsOldestFirst() {
    User organizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Priya").email("priya@example.com").build());

    WebhookEvent older = webhookEventRepository.saveAndFlush(anEvent(
        organizer.getId(), WebhookEventType.EVENT_PUBLISHED,
        Map.of("eventId", "abc"), Instant.parse("2026-01-01T00:00:00Z")));
    WebhookEvent newer = webhookEventRepository.saveAndFlush(anEvent(
        organizer.getId(), WebhookEventType.TICKET_PURCHASED,
        Map.of("ticketId", "def"), Instant.parse("2026-01-02T00:00:00Z")));

    // Scoped to this test's own organizer: Surefire shares one H2 database
    // across the whole test-class list in this suite, and
    // TicketPurchaseConcurrencyTest deliberately commits real, never-
    // rolled-back ticket.purchased events (proving genuine cross-thread
    // locking), which also write ticket.purchased WebhookEvent outbox rows
    // now that the outbox is wired up. Our hardcoded 2026-01-01/02
    // timestamps sort ahead of any of those (created with Instant.now()),
    // so they're always first -- but an unfiltered assertion would still
    // see the leftover rows trailing behind them and fail.
    var unfannedOut = webhookEventRepository.findUnfannedOut(PageRequest.of(0, 10)).stream()
        .filter(event -> event.getOrganizerId().equals(organizer.getId()))
        .toList();

    assertThat(unfannedOut).extracting(WebhookEvent::getId)
        .containsExactly(older.getId(), newer.getId());
    assertThat(unfannedOut.get(1).getPayload()).containsEntry("ticketId", "def");
  }

  private WebhookEvent anEvent(
      UUID organizerId, WebhookEventType type, Map<String, Object> payload, Instant occurredAt) {
    return WebhookEvent.builder()
        .organizerId(organizerId)
        .type(type)
        .payload(payload)
        .occurredAt(occurredAt)
        .build();
  }
}
