package com.showhop.api.repository;

import com.showhop.api.entity.Event;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, UUID> {

  Page<Event> findByOrganizerId(UUID organizerId, Pageable pageable);

  Optional<Event> findByIdAndOrganizerId(UUID id, UUID organizerId);
}
