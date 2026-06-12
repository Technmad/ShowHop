package com.showhop.api.service;

import com.showhop.api.dto.EventRequestDto;
import com.showhop.api.entity.Event;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventService {

  Event createEvent(UUID organizerId, EventRequestDto request);

  Page<Event> listEventsForOrganizer(UUID organizerId, Pageable pageable);

  Optional<Event> getEventForOrganizer(UUID organizerId, UUID eventId);

  Event updateEventForOrganizer(UUID organizerId, UUID eventId, EventRequestDto request);

  void deleteEventForOrganizer(UUID organizerId, UUID eventId);
}
