package com.showhop.api.entity;

import com.showhop.api.entity.enums.WebhookEndpointStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A URL an organizer wants domain events pushed to. {@code secret} is the
 * per-endpoint HMAC key used to sign outbound deliveries (see
 * {@code WebhookSigner}) -- never returned in a response DTO once set.
 */
@Entity
@Table(name = "webhook_endpoints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, of = "id")
public class WebhookEndpoint extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "organizer_id", nullable = false)
  private UUID organizerId;

  @Column(name = "url", nullable = false)
  private String url;

  @Column(name = "secret", nullable = false)
  private String secret;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "subscribed_event_types", nullable = false)
  private List<String> subscribedEventTypes;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  @Builder.Default
  private WebhookEndpointStatus status = WebhookEndpointStatus.ACTIVE;

  @Column(name = "consecutive_failures", nullable = false)
  @Builder.Default
  private int consecutiveFailures = 0;

  @Column(name = "circuit_opened_at")
  private Instant circuitOpenedAt;
}
