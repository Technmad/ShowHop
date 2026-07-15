package com.showhop.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One row per Razorpay event id we've handled -- the inbound-idempotency
 * ledger (PRD &sect;4.2). Keyed by Razorpay's own event id, not a generated
 * UUID, since the whole point is a fast existence check on that exact id.
 */
@Entity
@Table(name = "processed_razorpay_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "razorpayEventId")
public class ProcessedRazorpayEvent {

  @Id
  @Column(name = "razorpay_event_id", nullable = false, updatable = false)
  private String razorpayEventId;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;
}
