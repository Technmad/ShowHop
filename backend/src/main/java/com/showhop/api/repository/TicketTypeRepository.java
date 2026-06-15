package com.showhop.api.repository;

import com.showhop.api.entity.TicketType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {

  Page<TicketType> findByEventId(UUID eventId, Pageable pageable);

  Optional<TicketType> findByIdAndEventId(UUID id, UUID eventId);
}
