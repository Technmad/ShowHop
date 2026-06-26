package com.showhop.api.repository;

import com.showhop.api.entity.WebhookEvent;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

  /**
   * Candidate events for the fan-out relay: written by a committed domain
   * transaction (the outbox guarantee) but not yet fanned out to
   * subscribers. Locked so two relay instances running concurrently don't
   * both fan out the same event; {@code pageable} bounds the batch so the
   * lock is never held over an unbounded scan.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from WebhookEvent e where e.fannedOutAt is null order by e.occurredAt asc")
  List<WebhookEvent> findUnfannedOut(Pageable pageable);

  long countByOrganizerId(UUID organizerId);
}
