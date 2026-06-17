package com.showhop.api.repository;

import com.showhop.api.entity.Ticket;
import com.showhop.api.entity.enums.TicketStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

  /**
   * Status-aware on purpose: a CANCELLED ticket must not count against
   * capacity, or cancellations would never free inventory.
   */
  int countByTicketTypeIdAndStatus(UUID ticketTypeId, TicketStatus status);

  Optional<Ticket> findByIdAndPurchaserId(UUID id, UUID purchaserId);
}
