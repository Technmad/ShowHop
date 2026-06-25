package com.showhop.api.entity;

import com.showhop.api.entity.enums.WebhookEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An immutable record of something that happened, written **inside the same
 * transaction as the domain change it describes** -- the transactional
 * outbox write. A rolled-back domain transaction writes no row here; a
 * committed one is guaranteed to have exactly one. Fan-out to subscribed
 * {@link WebhookEndpoint}s (creating {@link WebhookDelivery} rows) happens
 * later, out of band, driven by {@code fannedOutAt} rather than by this
 * write itself.
 */
@Entity
@Table(name = "webhook_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, of = "id")
public class WebhookEvent extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "organizer_id", nullable = false)
  private UUID organizerId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private WebhookEventType type;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false)
  private Map<String, Object> payload;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "fanned_out_at")
  private Instant fannedOutAt;
}
