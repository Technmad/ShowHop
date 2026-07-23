package com.showhop.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An immutable "who did what, and when" record (PRD &sect;4.4) -- never
 * updated after insert, so unlike most entities here it has no
 * {@code BaseEntity}/{@code updatedAt}; {@code occurredAt} is the one
 * timestamp that matters. {@code actorUserId} is {@code null} for the one
 * system-triggered action this app audits (the paid-but-expired
 * auto-refund in {@code RazorpayWebhookServiceImpl}) -- there is no human
 * actor for that one.
 */
@Entity
@Table(name = "audit_log_entries")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class AuditLogEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "actor_user_id")
  private UUID actorUserId;

  @Column(name = "organizer_id", nullable = false)
  private UUID organizerId;

  @Column(name = "action", nullable = false)
  private String action;

  @Column(name = "entity_type", nullable = false)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private String entityId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata")
  private Map<String, Object> metadata;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;
}
