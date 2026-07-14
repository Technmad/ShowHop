package com.showhop.api.repository;

import com.showhop.api.entity.TicketReservation;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketReservationRepository extends JpaRepository<TicketReservation, UUID> {

  Optional<TicketReservation> findByIdempotencyKey(String idempotencyKey);

  /**
   * Joins ticketType eagerly: with open-in-view disabled, mapping this into
   * a response DTO happens after the repository's own transaction has
   * closed, so the association must already be initialized rather than
   * left as a lazy proxy (same reasoning as
   * {@code TicketRepository.findByIdAndPurchaserId}).
   */
  @Query("""
      select r from TicketReservation r
      join fetch r.ticketType
      where r.id = :id and r.buyer.id = :buyerId
      """)
  Optional<TicketReservation> findByIdAndBuyerId(@Param("id") UUID id, @Param("buyerId") UUID buyerId);

  /**
   * Locks the row for the duration of the caller's transaction -- used at
   * fulfillment time to re-assert the reservation is still HELD and
   * unexpired before a Ticket is created, the same lock discipline
   * {@code TicketTypeRepository.findByIdWithLock} already validates.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from TicketReservation r where r.id = :id")
  Optional<TicketReservation> findByIdWithLock(@Param("id") UUID id);

  /**
   * The "activeHolds" term in {@code available = totalAvailable - sold -
   * activeHolds} (PRD &sect;4.2). Counts only unexpired HELD rows -- an
   * expired-but-not-yet-reaped hold must not still block inventory.
   */
  @Query("""
      select count(r) from TicketReservation r
      where r.ticketType.id = :ticketTypeId and r.state = 'HELD' and r.expiresAt > CURRENT_TIMESTAMP
      """)
  int countActiveHolds(@Param("ticketTypeId") UUID ticketTypeId);

  /**
   * Claims expired HELD reservations for the reaper. {@code FOR UPDATE
   * SKIP LOCKED} lets concurrent reaper ticks (or a reaper racing a
   * fulfillment webhook re-asserting the same row) each get a disjoint
   * set instead of blocking on one another -- same competing-consumers
   * primitive as {@code WebhookDeliveryRepository.findClaimable}. Native
   * SQL because SKIP LOCKED has no JPQL/JPA lock-mode equivalent.
   */
  @Query(value = """
      SELECT * FROM ticket_reservations
      WHERE state = 'HELD' AND expires_at <= now()
      ORDER BY expires_at
      LIMIT :limit
      FOR UPDATE SKIP LOCKED
      """, nativeQuery = true)
  List<TicketReservation> findExpiredHeld(@Param("limit") int limit);
}
