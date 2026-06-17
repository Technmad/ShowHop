package com.showhop.api.repository;

import com.showhop.api.entity.TicketType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {

  Page<TicketType> findByEventId(UUID eventId, Pageable pageable);

  Optional<TicketType> findByIdAndEventId(UUID id, UUID eventId);

  /**
   * Locks the row for the duration of the caller's transaction, so a
   * concurrent purchase against the same ticket type blocks until this one
   * commits or rolls back -- the mechanism the oversell-safety guarantee
   * in {@code TicketPurchaseService} depends on.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from TicketType t where t.id = :id")
  Optional<TicketType> findByIdWithLock(@Param("id") UUID id);
}
