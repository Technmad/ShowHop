package com.showhop.api.repository;

import com.showhop.api.entity.Event;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, UUID> {
}
