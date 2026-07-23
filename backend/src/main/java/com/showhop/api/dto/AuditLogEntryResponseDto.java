package com.showhop.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogEntryResponseDto(
    UUID id,
    UUID actorUserId,
    String action,
    String entityType,
    String entityId,
    Map<String, Object> metadata,
    Instant occurredAt) {
}
