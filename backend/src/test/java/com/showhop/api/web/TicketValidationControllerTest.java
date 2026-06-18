package com.showhop.api.web;

import static com.showhop.api.testsupport.JwtTestSupport.authenticatedAs;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showhop.api.dto.EventRequestDto;
import com.showhop.api.dto.TicketTypeRequestDto;
import com.showhop.api.dto.TicketValidationRequestDto;
import com.showhop.api.entity.enums.EventStatus;
import com.showhop.api.entity.enums.TicketValidationMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class TicketValidationControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void aTicketScansValidOnceThenInvalidOnASecondAttempt() throws Exception {
    RequestPostProcessor organizer = authenticatedAs("ORGANIZER");
    RequestPostProcessor attendee = authenticatedAs("ATTENDEE");
    RequestPostProcessor staff = authenticatedAs("STAFF");

    UUID eventId = createPublishedEvent(organizer);
    UUID ticketTypeId = createTicketType(organizer, eventId);
    UUID ticketId = purchaseTicket(attendee, eventId, ticketTypeId);

    TicketValidationRequestDto request =
        new TicketValidationRequestDto(ticketId, TicketValidationMethod.QR_SCAN);

    mockMvc.perform(post("/api/v1/ticket-validations")
            .with(staff)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("VALID"));

    // Same ticket, scanned again -- must not admit twice.
    mockMvc.perform(post("/api/v1/ticket-validations")
            .with(staff)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INVALID"));
  }

  @Test
  void onlyStaffCanValidateTickets() throws Exception {
    RequestPostProcessor attendee = authenticatedAs("ATTENDEE");
    TicketValidationRequestDto request =
        new TicketValidationRequestDto(UUID.randomUUID(), TicketValidationMethod.MANUAL);

    mockMvc.perform(post("/api/v1/ticket-validations")
            .with(attendee)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  private UUID createPublishedEvent(RequestPostProcessor organizer) throws Exception {
    Instant now = Instant.now();
    EventRequestDto request = new EventRequestDto(
        "Autumn Tech Meetup", "Riverside Hall", now.plusSeconds(3600), now.plusSeconds(7200),
        null, null, EventStatus.PUBLISHED);
    String json = mockMvc.perform(post("/api/v1/events")
            .with(organizer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return UUID.fromString(objectMapper.readTree(json).get("id").asText());
  }

  private UUID createTicketType(RequestPostProcessor organizer, UUID eventId) throws Exception {
    TicketTypeRequestDto request =
        new TicketTypeRequestDto("General Admission", null, new BigDecimal("29.99"), 100);
    String json = mockMvc.perform(post("/api/v1/events/" + eventId + "/ticket-types")
            .with(organizer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return UUID.fromString(objectMapper.readTree(json).get("id").asText());
  }

  private UUID purchaseTicket(RequestPostProcessor buyer, UUID eventId, UUID ticketTypeId)
      throws Exception {
    String json = mockMvc.perform(post(
            "/api/v1/published-events/" + eventId + "/ticket-types/" + ticketTypeId + "/tickets")
            .with(buyer))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return UUID.fromString(objectMapper.readTree(json).get("id").asText());
  }
}
