package com.showhop.api.repository;

import com.showhop.api.entity.Event;
import com.showhop.api.entity.enums.EventStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, UUID> {

  Page<Event> findByOrganizerId(UUID organizerId, Pageable pageable);

  Optional<Event> findByIdAndOrganizerId(UUID id, UUID organizerId);

  Page<Event> findByStatus(EventStatus status, Pageable pageable);

  Optional<Event> findByIdAndStatus(UUID id, EventStatus status);

  @Query("""
      select e from Event e
      where e.status = com.showhop.api.entity.enums.EventStatus.PUBLISHED
        and (lower(e.name) like lower(concat('%', :query, '%'))
          or lower(e.venue) like lower(concat('%', :query, '%')))
      """)
  Page<Event> searchPublished(@Param("query") String query, Pageable pageable);
}
