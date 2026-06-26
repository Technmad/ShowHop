package com.showhop.api.repository;

import com.showhop.api.entity.WebhookDelivery;
import com.showhop.api.entity.enums.WebhookDeliveryState;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

  Page<WebhookDelivery> findByEndpointIdOrderByCreatedAtDesc(UUID endpointId, Pageable pageable);

  /** Used to avoid queuing a second half-open probe while one is still in flight. */
  boolean existsByEndpointIdAndProbeTrueAndStateNotIn(
      UUID endpointId, List<WebhookDeliveryState> terminalStates);

  /**
   * Claims due deliveries for this worker: rows either newly PENDING/RETRYING
   * and due now, or IN_FLIGHT with an expired lease (the previous claimant
   * crashed mid-send). {@code FOR UPDATE SKIP LOCKED} means concurrent
   * workers each get a disjoint batch instead of racing over the same rows
   * -- the competing-consumers claim this table is built around. Native SQL
   * because SKIP LOCKED has no JPQL/JPA lock-mode equivalent; Postgres-only
   * by design, exercised in {@code WebhookDeliveryWorkerPostgresIT}.
   */
  @Query(value = """
      SELECT * FROM webhook_deliveries
      WHERE (state IN ('PENDING', 'RETRYING') AND (next_retry_at IS NULL OR next_retry_at <= now()))
         OR (state = 'IN_FLIGHT' AND locked_until < now())
      ORDER BY next_retry_at NULLS FIRST
      LIMIT :limit
      FOR UPDATE SKIP LOCKED
      """, nativeQuery = true)
  List<WebhookDelivery> findClaimable(@Param("limit") int limit);
}
