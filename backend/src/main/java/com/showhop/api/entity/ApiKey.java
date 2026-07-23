package com.showhop.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * Programmatic, organizer-scoped credential for the webhook-management API
 * -- an alternate path alongside the Keycloak JWT session, not a
 * replacement for it (PRD &sect;4.4). Only {@code hashedKey} (SHA-256) and
 * {@code keyPrefix} (an 8-char lookup prefix, indexed) are stored; the raw
 * key is generated once in {@code ApiKeyServiceImpl} and never persisted,
 * so it can only ever be shown in the create response.
 */
@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, of = "id")
public class ApiKey extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "organizer_id", nullable = false)
  private UUID organizerId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "key_prefix", nullable = false)
  private String keyPrefix;

  @Column(name = "hashed_key", nullable = false)
  private String hashedKey;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;
}
