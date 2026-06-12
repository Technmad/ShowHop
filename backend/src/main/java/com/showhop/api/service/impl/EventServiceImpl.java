package com.showhop.api.service.impl;

import com.showhop.api.dto.EventRequestDto;
import com.showhop.api.entity.Event;
import com.showhop.api.entity.User;
import com.showhop.api.exception.EventNotFoundException;
import com.showhop.api.exception.UserNotFoundException;
import com.showhop.api.mapper.EventMapper;
import com.showhop.api.repository.EventRepository;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.service.EventService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

  private final EventRepository eventRepository;
  private final UserRepository userRepository;
  private final EventMapper eventMapper;

  @Override
  @Transactional
  public Event createEvent(UUID organizerId, EventRequestDto request) {
    User organizer = userRepository.findById(organizerId)
        .orElseThrow(() -> new UserNotFoundException(
            "User with id '%s' was not found".formatted(organizerId)));

    Event event = eventMapper.toEntity(request);
    event.setOrganizer(organizer);

    return eventRepository.save(event);
  }

  @Override
  public Page<Event> listEventsForOrganizer(UUID organizerId, Pageable pageable) {
    return eventRepository.findByOrganizerId(organizerId, pageable);
  }

  @Override
  public Optional<Event> getEventForOrganizer(UUID organizerId, UUID eventId) {
    return eventRepository.findByIdAndOrganizerId(eventId, organizerId);
  }

  @Override
  @Transactional
  public Event updateEventForOrganizer(UUID organizerId, UUID eventId, EventRequestDto request) {
    Event event = eventRepository.findByIdAndOrganizerId(eventId, organizerId)
        .orElseThrow(() -> new EventNotFoundException(
            "Event with id '%s' was not found".formatted(eventId)));

    eventMapper.updateEntityFromDto(request, event);

    return eventRepository.save(event);
  }

  @Override
  @Transactional
  public void deleteEventForOrganizer(UUID organizerId, UUID eventId) {
    eventRepository.findByIdAndOrganizerId(eventId, organizerId)
        .ifPresent(eventRepository::delete);
  }
}
