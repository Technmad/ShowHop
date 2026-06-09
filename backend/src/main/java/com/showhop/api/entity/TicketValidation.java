package com.showhop.api.entity;

import com.showhop.api.entity.enums.TicketValidationMethod;
import com.showhop.api.entity.enums.TicketValidationStatus;
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
 * One attempt by staff to admit a ticket at the door, by QR scan or manual
 * override. A ticket can accumulate several of these (e.g. a rejected scan
 * followed by a manual override) -- it's an append-only log, not a single
 * mutable status on the ticket.
 */
@Entity
@Table(name = "ticket_validations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, of = "id")
public class TicketValidation extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ticket_id")
  private Ticket ticket;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "validated_by_id")
  private User validatedBy;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private TicketValidationStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "method", nullable = false)
  private TicketValidationMethod method;

  @Column(name = "validated_at", nullable = false)
  private Instant validatedAt;
}
