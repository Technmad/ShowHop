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
 * One row per inbound Razorpay event we've handled -- the inbound-idempotency
 * ledger (PRD &sect;4.2). Razorpay's classic webhook payload carries no
 * top-level, stable event id (unlike Stripe), so {@code razorpayEventId} is
 * actually a synthesized {@code eventType:paymentId} key, not a field
 * Razorpay sends directly -- still a fast existence check on that key, not
 * a generated UUID, which is the property that matters here.
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
