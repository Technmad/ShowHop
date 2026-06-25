package com.showhop.api.entity;

import com.showhop.api.entity.enums.WebhookDeliveryState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * One event's delivery journey to one endpoint -- not one HTTP attempt.
 * {@code attempt} counts tries on this row; a terminal row ({@code SUCCEEDED}
 * / {@code DEAD_LETTER}) is never mutated further -- replay creates a new
 * row for the same (event, endpoint) pair instead.
 *
 * <p>{@code lockedBy}/{@code lockedUntil} are a claim lease: a worker takes
 * one via a {@code FOR UPDATE SKIP LOCKED} claim query (see
 * {@code WebhookDeliveryRepository.claimDue}), commits the claim, then sends
 * the HTTP request with no DB transaction open. If the worker dies mid-send,
 * the lease expires and another worker can re-claim the row -- at-least-once,
 * by design (see the receiver idempotency contract in the PRD).
 */
@Entity
@Table(name = "webhook_deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, of = "id")
public class WebhookDelivery extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "endpoint_id")
  private WebhookEndpoint endpoint;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id")
  private WebhookEvent event;

  @Enumerated(EnumType.STRING)
  @Column(name = "state", nullable = false)
  @Builder.Default
  private WebhookDeliveryState state = WebhookDeliveryState.PENDING;

  @Column(name = "attempt", nullable = false)
  @Builder.Default
  private int attempt = 0;

  @Column(name = "max_attempts", nullable = false)
  @Builder.Default
  private int maxAttempts = 8;

  @Column(name = "is_probe", nullable = false)
  @Builder.Default
  private boolean probe = false;

  @Column(name = "next_retry_at")
  private Instant nextRetryAt;

  @Column(name = "locked_by")
  private String lockedBy;

  @Column(name = "locked_until")
  private Instant lockedUntil;

  @Column(name = "last_response_code")
  private Integer lastResponseCode;

  @Column(name = "last_error")
  private String lastError;
}
