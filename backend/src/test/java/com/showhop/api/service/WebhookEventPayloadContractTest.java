package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.dto.EventRequestDto;
import com.showhop.api.dto.TicketTypeRequestDto;
import com.showhop.api.entity.Ticket;
import com.showhop.api.entity.User;
import com.showhop.api.entity.WebhookEvent;
import com.showhop.api.entity.enums.EventStatus;
import com.showhop.api.entity.enums.TicketValidationMethod;
import com.showhop.api.entity.enums.WebhookEventType;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.repository.WebhookEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pins the payload shape of every WebhookEvent this app publishes, exercised
 * through the real domain services (not mocks) so a change to any of these
 * field sets is caught here, in one place, rather than discovered by a
 * confused integrator whose receiver silently stopped matching a field. A
 * deliberate change to a payload should mean a deliberate edit to this test,
 * not a surprise.
 */
@SpringBootTest
@Transactional
class WebhookEventPayloadContractTest {

  @Autowired private EventService eventService;
  @Autowired private TicketTypeService ticketTypeService;
  @Autowired private TicketPurchaseService ticketPurchaseService;
  @Autowired private TicketValidationService ticketValidationService;
  @Autowired private UserRepository userRepository;
  @Autowired private WebhookEventRepository webhookEventRepository;

  @Test
  void eventPublishedPayloadContainsExactlyEventIdNameAndVenue() {
    UUID organizerId = anOrganizer();
    UUID eventId = eventService.createEvent(organizerId, anEventRequest(EventStatus.DRAFT)).getId();

    eventService.updateEventForOrganizer(organizerId, eventId, anEventRequest(EventStatus.PUBLISHED));

    WebhookEvent published = onlyEventOfType(organizerId, WebhookEventType.EVENT_PUBLISHED);
    assertThat(published.getPayload().keySet())
        .containsExactlyInAnyOrder("eventId", "name", "venue");
    assertThat(published.getPayload().get("eventId")).isEqualTo(eventId.toString());
    assertThat(published.getPayload().get("name")).isEqualTo("Autumn Tech Meetup");
    assertThat(published.getPayload().get("venue")).isEqualTo("Riverside Hall");
  }

  @Test
  void ticketPurchasedPayloadContainsExactlyTheFourIds() {
    UUID organizerId = anOrganizer();
    UUID buyerId = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Buyer").email("buyer-" + UUID.randomUUID() + "@example.com")
        .build()).getId();
    UUID eventId = eventService.createEvent(organizerId, anEventRequest(EventStatus.PUBLISHED)).getId();
    UUID ticketTypeId = ticketTypeService.createTicketType(organizerId, eventId,
        new TicketTypeRequestDto("General Admission", null, new BigDecimal("29.99"), 10)).getId();

    Ticket ticket = ticketPurchaseService.purchaseTicket(buyerId, eventId, ticketTypeId);

    WebhookEvent purchased = onlyEventOfType(organizerId, WebhookEventType.TICKET_PURCHASED);
    assertThat(purchased.getPayload().keySet())
        .containsExactlyInAnyOrder("ticketId", "ticketTypeId", "eventId", "purchaserId");
    assertThat(purchased.getPayload().get("ticketId")).isEqualTo(ticket.getId().toString());
    assertThat(purchased.getPayload().get("ticketTypeId")).isEqualTo(ticketTypeId.toString());
    assertThat(purchased.getPayload().get("eventId")).isEqualTo(eventId.toString());
    assertThat(purchased.getPayload().get("purchaserId")).isEqualTo(buyerId.toString());
  }

  @Test
  void ticketValidatedPayloadContainsExactlyTicketIdStatusAndMethod() {
    UUID organizerId = anOrganizer();
    UUID staffId = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Staff").email("staff-" + UUID.randomUUID() + "@example.com")
        .build()).getId();
    UUID buyerId = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Buyer").email("buyer-" + UUID.randomUUID() + "@example.com")
        .build()).getId();
    UUID eventId = eventService.createEvent(organizerId, anEventRequest(EventStatus.PUBLISHED)).getId();
    UUID ticketTypeId = ticketTypeService.createTicketType(organizerId, eventId,
        new TicketTypeRequestDto("General Admission", null, new BigDecimal("29.99"), 10)).getId();
    Ticket ticket = ticketPurchaseService.purchaseTicket(buyerId, eventId, ticketTypeId);

    ticketValidationService.validateTicket(staffId, ticket.getId(), TicketValidationMethod.QR_SCAN);

    WebhookEvent validated = onlyEventOfType(organizerId, WebhookEventType.TICKET_VALIDATED);
    assertThat(validated.getPayload().keySet())
        .containsExactlyInAnyOrder("ticketId", "status", "method");
    assertThat(validated.getPayload().get("ticketId")).isEqualTo(ticket.getId().toString());
    assertThat(validated.getPayload().get("status")).isEqualTo("VALID");
    assertThat(validated.getPayload().get("method")).isEqualTo("QR_SCAN");
  }

  /**
   * Scoped to this test's own randomly-generated organizerId, not just the
   * event type -- the H2 database backing this test class is shared across
   * the whole Surefire run in one JVM, and TicketPurchaseConcurrencyTest in
   * particular commits real, never-rolled-back TICKET_PURCHASED events
   * (deliberately, since it's proving genuine cross-thread commits). An
   * unscoped findAll() would flake depending on test execution order.
   */
  private WebhookEvent onlyEventOfType(UUID organizerId, WebhookEventType type) {
    var matches = webhookEventRepository.findAll().stream()
        .filter(event -> event.getOrganizerId().equals(organizerId) && event.getType() == type)
        .toList();
    assertThat(matches).as("exactly one %s event for this organizer", type).hasSize(1);
    return matches.get(0);
  }

  private UUID anOrganizer() {
    return userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Organizer")
        .email("organizer-" + UUID.randomUUID() + "@example.com").build()).getId();
  }

  private EventRequestDto anEventRequest(EventStatus status) {
    Instant now = Instant.now();
    return new EventRequestDto(
        "Autumn Tech Meetup", "Riverside Hall", now.plusSeconds(3600), now.plusSeconds(7200),
        null, null, status);
  }
}
