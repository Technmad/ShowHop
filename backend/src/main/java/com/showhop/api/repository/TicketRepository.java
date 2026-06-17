package com.showhop.api.repository;

import com.showhop.api.entity.Ticket;
import com.showhop.api.entity.enums.TicketStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

  /**
   * Status-aware on purpose: a CANCELLED ticket must not count against
   * capacity, or cancellations would never free inventory.
   */
  int countByTicketTypeIdAndStatus(UUID ticketTypeId, TicketStatus status);

  /**
   * Joins ticketType and its event eagerly: with open-in-view disabled,
   * mapping this into a response DTO happens after the repository's own
   * transaction has closed, so those associations must already be
   * initialized rather than left as lazy proxies.
   */
  @Query("""
      select t from Ticket t
      join fetch t.ticketType tt
      join fetch tt.event
      where t.id = :id and t.purchaser.id = :purchaserId
      """)
  Optional<Ticket> findByIdAndPurchaserId(@Param("id") UUID id, @Param("purchaserId") UUID purchaserId);

  @Query(
      value = """
          select t from Ticket t
          join fetch t.ticketType tt
          join fetch tt.event
          where t.purchaser.id = :purchaserId
          """,
      countQuery = "select count(t) from Ticket t where t.purchaser.id = :purchaserId")
  Page<Ticket> findByPurchaserId(@Param("purchaserId") UUID purchaserId, Pageable pageable);
}
