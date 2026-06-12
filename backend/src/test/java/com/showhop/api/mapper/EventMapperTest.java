package com.showhop.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.dto.EventRequestDto;
import com.showhop.api.dto.EventResponseDto;
import com.showhop.api.entity.Event;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.EventStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventMapperTest {

  private final EventMapper mapper = new EventMapperImpl();

  @Test
  void mapsARequestDtoToANewEntityLeavingOwnershipAndAuditFieldsUnset() {
    Instant now = Instant.now();
    EventRequestDto dto = new EventRequestDto(
        "Autumn Tech Meetup", "Riverside Hall",
        now.plus(30, ChronoUnit.DAYS), now.plus(30, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS),
        null, null, EventStatus.DRAFT);

    Event event = mapper.toEntity(dto);

    assertThat(event.getId()).isNull();
    assertThat(event.getOrganizer()).isNull();
    assertThat(event.getName()).isEqualTo("Autumn Tech Meetup");
    assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
  }

  @Test
  void mapsAnEntityToAResponseDtoFlatteningTheOrganizerToItsId() {
    UUID organizerId = UUID.randomUUID();
    Event event = Event.builder()
        .id(UUID.randomUUID())
        .name("Autumn Tech Meetup")
        .venue("Riverside Hall")
        .startsAt(Instant.now())
        .endsAt(Instant.now())
        .status(EventStatus.PUBLISHED)
        .organizer(User.builder().id(organizerId).build())
        .build();

    EventResponseDto dto = mapper.toResponseDto(event);

    assertThat(dto.id()).isEqualTo(event.getId());
    assertThat(dto.organizerId()).isEqualTo(organizerId);
    assertThat(dto.status()).isEqualTo(EventStatus.PUBLISHED);
  }

  @Test
  void updatesAnExistingEntityInPlaceWithoutTouchingOwnershipOrId() {
    UUID id = UUID.randomUUID();
    User organizer = User.builder().id(UUID.randomUUID()).build();
    Event existing = Event.builder()
        .id(id)
        .name("Old Name")
        .venue("Old Venue")
        .startsAt(Instant.now())
        .endsAt(Instant.now())
        .status(EventStatus.DRAFT)
        .organizer(organizer)
        .build();

    Instant now = Instant.now();
    EventRequestDto dto = new EventRequestDto(
        "New Name", "New Venue", now, now.plus(1, ChronoUnit.HOURS),
        null, null, EventStatus.PUBLISHED);

    mapper.updateEntityFromDto(dto, existing);

    assertThat(existing.getId()).isEqualTo(id);
    assertThat(existing.getOrganizer()).isSameAs(organizer);
    assertThat(existing.getName()).isEqualTo("New Name");
    assertThat(existing.getStatus()).isEqualTo(EventStatus.PUBLISHED);
  }
}
