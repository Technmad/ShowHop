package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.showhop.api.entity.AuditLogEntry;
import com.showhop.api.repository.AuditLogEntryRepository;
import com.showhop.api.service.impl.AuditLogServiceImpl;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

  @Mock
  private AuditLogEntryRepository auditLogEntryRepository;

  @InjectMocks
  private AuditLogServiceImpl auditLogService;

  @Test
  void recordSavesAnEntryWithAllTheGivenFields() {
    UUID actorId = UUID.randomUUID();
    UUID organizerId = UUID.randomUUID();
    when(auditLogEntryRepository.save(any(AuditLogEntry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    auditLogService.record(actorId, organizerId, "EVENT_DELETED", "Event", "event-1", Map.of("k", "v"));

    ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
    org.mockito.Mockito.verify(auditLogEntryRepository).save(captor.capture());
    AuditLogEntry saved = captor.getValue();
    assertThat(saved.getActorUserId()).isEqualTo(actorId);
    assertThat(saved.getOrganizerId()).isEqualTo(organizerId);
    assertThat(saved.getAction()).isEqualTo("EVENT_DELETED");
    assertThat(saved.getEntityType()).isEqualTo("Event");
    assertThat(saved.getEntityId()).isEqualTo("event-1");
    assertThat(saved.getMetadata()).containsEntry("k", "v");
    assertThat(saved.getOccurredAt()).isNotNull();
  }

  @Test
  void recordAllowsANullActorForSystemTriggeredActions() {
    when(auditLogEntryRepository.save(any(AuditLogEntry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    auditLogService.record(null, UUID.randomUUID(), "RESERVATION_AUTO_REFUNDED",
        "TicketReservation", "reservation-1", null);

    ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
    org.mockito.Mockito.verify(auditLogEntryRepository).save(captor.capture());
    assertThat(captor.getValue().getActorUserId()).isNull();
  }

  @Test
  void listForOrganizerDelegatesToTheOrganizerScopedRepositoryQuery() {
    UUID organizerId = UUID.randomUUID();
    Pageable pageable = Pageable.unpaged();
    when(auditLogEntryRepository.findByOrganizerIdOrderByOccurredAtDesc(organizerId, pageable))
        .thenReturn(new PageImpl<>(java.util.List.of()));

    assertThat(auditLogService.listForOrganizer(organizerId, pageable)).isEmpty();
  }
}
