package com.showhop.api.entity;

import com.showhop.api.entity.enums.ReservationState;
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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A short-lived claim on {@link TicketType} inventory while a Razorpay
 * payment is in flight (PRD &sect;4.2). Capacity is committed here, at
 * reservation time, under the same {@code PESSIMISTIC_WRITE} lock the
 * synchronous purchase path already uses -- {@code Ticket} rows are only
 * created once the inbound webhook confirms payment and re-asserts this
 * reservation is still {@link ReservationState#HELD} and unexpired.
 */
@Entity
@Table(name = "ticket_reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, of = "id")
public class TicketReservation extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ticket_type_id")
  private TicketType ticketType;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "buyer_id")
  private User buyer;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @Enumerated(EnumType.STRING)
  @Column(name = "state", nullable = false)
  private ReservationState state;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  /** Set once the Razorpay Order is created; null only in the instant between insert and that call. */
  @Column(name = "razorpay_order_id")
  private String razorpayOrderId;

  /** Set once a {@code payment.captured}/{@code payment.failed} webhook names the payment. */
  @Column(name = "razorpay_payment_id")
  private String razorpayPaymentId;

  /**
   * Client-supplied key from the {@code POST .../reservations} call. Unique
   * so a retried initiation is caught by the database, not just the
   * service-layer check -- the same discipline the outbound webhook engine
   * uses for its own idempotency guarantees.
   */
  @Column(name = "idempotency_key", nullable = false, unique = true)
  private String idempotencyKey;
}
