package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.showhop.api.dto.EventRequestDto;
import com.showhop.api.entity.Event;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.EventStatus;
import com.showhop.api.exception.EventNotFoundException;
import com.showhop.api.exception.UserNotFoundException;
import com.showhop.api.mapper.EventMapper;
import com.showhop.api.repository.EventRepository;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.service.impl.EventServiceImpl;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

  @Mock
  private EventRepository eventRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private EventMapper eventMapper;
  @Mock
  private WebhookEventPublisher webhookEventPublisher;
  @Mock
  private AuditLogService auditLogService;

  @InjectMocks
  private EventServiceImpl eventService;

  @Test
  void createEventAssignsTheRequestingUserAsOrganizer() {
    UUID organizerId = UUID.randomUUID();
    User organizer = User.builder().id(organizerId).build();
    EventRequestDto request = anEventRequest();
    Event mappedEvent = new Event();

    when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
    when(eventMapper.toEntity(request)).thenReturn(mappedEvent);
    when(eventRepository.save(mappedEvent)).thenReturn(mappedEvent);

    Event result = eventService.createEvent(organizerId, request);

    assertThat(result.getOrganizer()).isSameAs(organizer);
    verify(eventRepository).save(mappedEvent);
  }

  @Test
  void createEventFailsFastWhenTheOrganizerDoesNotExist() {
    UUID organizerId = UUID.randomUUID();
    when(userRepository.findById(organizerId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventService.createEvent(organizerId, anEventRequest()))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void updateEventForOrganizerAppliesChangesOnlyToAnEventTheyOwn() {
    UUID organizerId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    Event existing = Event.builder().id(eventId).build();
    EventRequestDto request = anEventRequest();

    when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
        .thenReturn(Optional.of(existing));
    when(eventRepository.save(existing)).thenReturn(existing);

    eventService.updateEventForOrganizer(organizerId, eventId, request);

    verify(eventMapper).updateEntityFromDto(request, existing);
    verify(eventRepository).save(existing);
  }

  @Test
  void updateEventForOrganizerPublishesAWebhookEventOnlyOnTheTransitionToPublished() {
    UUID organizerId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    Event draft = Event.builder()
        .id(eventId).name("Autumn Tech Meetup").venue("Riverside Hall")
        .status(EventStatus.DRAFT).build();
    EventRequestDto request = anEventRequest();

    when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
        .thenReturn(Optional.of(draft));
    org.mockito.Mockito.doAnswer(invocation -> {
      Event target = invocation.getArgument(1);
      target.setStatus(EventStatus.PUBLISHED);
      return null;
    }).when(eventMapper).updateEntityFromDto(request, draft);
    when(eventRepository.save(draft)).thenReturn(draft);

    eventService.updateEventForOrganizer(organizerId, eventId, request);

    verify(webhookEventPublisher).publish(
        eq(organizerId), eq(com.showhop.api.entity.enums.WebhookEventType.EVENT_PUBLISHED), any());
    verify(auditLogService).record(
        eq(organizerId), eq(organizerId), eq("EVENT_STATUS_CHANGED"), eq("Event"), eq(eventId.toString()), any());
  }

  @Test
  void updateEventForOrganizerDoesNotRepublishWhenAlreadyPublished() {
    UUID organizerId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    Event alreadyPublished = Event.builder().id(eventId).status(EventStatus.PUBLISHED).build();
    EventRequestDto request = anEventRequest();

    when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
        .thenReturn(Optional.of(alreadyPublished));
    when(eventRepository.save(alreadyPublished)).thenReturn(alreadyPublished);

    eventService.updateEventForOrganizer(organizerId, eventId, request);

    verify(webhookEventPublisher, org.mockito.Mockito.never())
        .publish(any(), any(), any());
  }

  @Test
  void updateEventForOrganizerRejectsAnEventTheyDoNotOwn() {
    UUID organizerId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> eventService.updateEventForOrganizer(organizerId, eventId, anEventRequest()))
        .isInstanceOf(EventNotFoundException.class);
  }

  @Test
  void deleteEventForOrganizerIsANoOpWhenTheEventIsNotTheirs() {
    UUID organizerId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
        .thenReturn(Optional.empty());

    eventService.deleteEventForOrganizer(organizerId, eventId);

    verify(eventRepository, org.mockito.Mockito.never()).delete(any());
    verify(auditLogService, org.mockito.Mockito.never()).record(any(), any(), any(), any(), any(), any());
  }

  @Test
  void deletingAnOwnedEventRecordsAnAuditEntry() {
    UUID organizerId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    Event existing = Event.builder().id(eventId).build();
    when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
        .thenReturn(Optional.of(existing));

    eventService.deleteEventForOrganizer(organizerId, eventId);

    verify(eventRepository).delete(existing);
    verify(auditLogService).record(
        eq(organizerId), eq(organizerId), eq("EVENT_DELETED"), eq("Event"), eq(eventId.toString()), any());
  }

  @Test
  void getPublishedEventOnlyEverReturnsPublishedEvents() {
    UUID eventId = UUID.randomUUID();
    when(eventRepository.findByIdAndStatus(eventId, EventStatus.PUBLISHED))
        .thenReturn(Optional.empty());

    assertThat(eventService.getPublishedEvent(eventId)).isEmpty();
    verify(eventRepository).findByIdAndStatus(eventId, EventStatus.PUBLISHED);
  }

  private EventRequestDto anEventRequest() {
    Instant now = Instant.now();
    return new EventRequestDto(
        "Autumn Tech Meetup", "Riverside Hall", now, now.plusSeconds(3600),
        null, null, EventStatus.DRAFT);
  }
}
