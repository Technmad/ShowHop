package com.showhop.api.service.impl;

import com.showhop.api.entity.AuditLogEntry;
import com.showhop.api.repository.AuditLogEntryRepository;
import com.showhop.api.service.AuditLogService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

  private final AuditLogEntryRepository auditLogEntryRepository;

  @Override
  @Transactional
  public void record(UUID actorUserId, UUID organizerId, String action, String entityType, String entityId,
      Map<String, Object> metadata) {
    auditLogEntryRepository.save(AuditLogEntry.builder()
        .actorUserId(actorUserId)
        .organizerId(organizerId)
        .action(action)
        .entityType(entityType)
        .entityId(entityId)
        .metadata(metadata)
        .occurredAt(Instant.now())
        .build());
  }

  @Override
  public Page<AuditLogEntry> listForOrganizer(UUID organizerId, Pageable pageable) {
    return auditLogEntryRepository.findByOrganizerIdOrderByOccurredAtDesc(organizerId, pageable);
  }
}
