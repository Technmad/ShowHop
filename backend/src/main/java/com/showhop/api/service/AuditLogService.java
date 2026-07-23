package com.showhop.api.service;

import com.showhop.api.entity.AuditLogEntry;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

  /**
   * Writes one entry. Deliberately plain and synchronous, not a queue --
   * this is a low-volume admin-action log, not the high-volume webhook
   * outbox. Call it from inside the same {@code @Transactional} method as
   * the action it records, so a rolled-back action never leaves a "this
   * happened" entry behind. {@code actorUserId} may be {@code null} for a
   * system-triggered action (e.g. the paid-but-expired auto-refund).
   */
  void record(UUID actorUserId, UUID organizerId, String action, String entityType, String entityId,
      Map<String, Object> metadata);

  Page<AuditLogEntry> listForOrganizer(UUID organizerId, Pageable pageable);
}
