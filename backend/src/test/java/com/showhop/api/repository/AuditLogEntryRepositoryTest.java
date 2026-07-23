package com.showhop.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.entity.AuditLogEntry;
import com.showhop.api.entity.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuditLogEntryRepositoryTest {

  @Autowired
  private AuditLogEntryRepository auditLogEntryRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void returnsOnlyTheGivenOrganizersEntriesNewestFirst() {
    User organizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Priya").email("priya-audit@example.com").build());
    User otherOrganizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Aman").email("aman-audit@example.com").build());

    auditLogEntryRepository.saveAndFlush(anEntry(organizer.getId(), "EVENT_DELETED", Instant.now().minusSeconds(60)));
    AuditLogEntry newest = auditLogEntryRepository.saveAndFlush(
        anEntry(organizer.getId(), "EVENT_STATUS_CHANGED", Instant.now()));
    auditLogEntryRepository.saveAndFlush(anEntry(otherOrganizer.getId(), "API_KEY_CREATED", Instant.now()));

    var page = auditLogEntryRepository.findByOrganizerIdOrderByOccurredAtDesc(
        organizer.getId(), Pageable.unpaged());

    assertThat(page.getContent()).hasSize(2);
    assertThat(page.getContent().get(0).getId()).isEqualTo(newest.getId());
  }

  private AuditLogEntry anEntry(UUID organizerId, String action, Instant occurredAt) {
    return AuditLogEntry.builder()
        .organizerId(organizerId).action(action).entityType("Event")
        .entityId(UUID.randomUUID().toString()).occurredAt(occurredAt).build();
  }
}
