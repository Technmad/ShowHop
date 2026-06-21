package com.showhop.api.web;

import static com.showhop.api.testsupport.JwtTestSupport.authenticatedAs;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showhop.api.dto.EventRequestDto;
import com.showhop.api.dto.TicketTypeRequestDto;
import com.showhop.api.entity.enums.EventStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class PublishedEventControllerTest {

  @Autowired
  private org.springframework.test.web.servlet.MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void aDraftEventIsInvisibleToThePublicUntilPublished() throws Exception {
    RequestPostProcessor organizer = authenticatedAs("ORGANIZER");
    UUID eventId = createEvent(organizer, EventStatus.DRAFT);

    // Unauthenticated -- no .with(...) at all.
    mockMvc.perform(get("/api/v1/published-events/" + eventId))
        .andExpect(status().isNotFound());
    mockMvc.perform(get("/api/v1/published-events?q=Autumn"))
        .andExpect(jsonPath("$.content", hasSize(0)));
    mockMvc.perform(get("/api/v1/published-events/" + eventId + "/ticket-types"))
        .andExpect(status().isNotFound());

    publish(organizer, eventId);
    createTicketType(organizer, eventId);

    mockMvc.perform(get("/api/v1/published-events/" + eventId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Autumn Tech Meetup"))
        // organizerId, salesStart/salesEnd, and audit timestamps are
        // internal fields -- the public DTO must not leak them.
        .andExpect(jsonPath("$.organizerId").doesNotExist());
    mockMvc.perform(get("/api/v1/published-events?q=autumn"))
        .andExpect(jsonPath("$.content", hasSize(1)));
    mockMvc.perform(get("/api/v1/published-events/" + eventId + "/ticket-types"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("General Admission"));
  }

  private void createTicketType(RequestPostProcessor organizer, UUID eventId) throws Exception {
    TicketTypeRequestDto request =
        new TicketTypeRequestDto("General Admission", null, new BigDecimal("29.99"), 100);
    mockMvc.perform(post("/api/v1/events/" + eventId + "/ticket-types")
            .with(organizer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  private UUID createEvent(RequestPostProcessor organizer, EventStatus status) throws Exception {
    Instant now = Instant.now();
    EventRequestDto request = new EventRequestDto(
        "Autumn Tech Meetup", "Riverside Hall", now.plusSeconds(3600), now.plusSeconds(7200),
        null, null, status);

    String json = mockMvc.perform(post("/api/v1/events")
            .with(organizer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    return UUID.fromString(objectMapper.readTree(json).get("id").asText());
  }

  private void publish(RequestPostProcessor organizer, UUID eventId) throws Exception {
    Instant now = Instant.now();
    EventRequestDto published = new EventRequestDto(
        "Autumn Tech Meetup", "Riverside Hall", now.plusSeconds(3600), now.plusSeconds(7200),
        null, null, EventStatus.PUBLISHED);

    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .put("/api/v1/events/" + eventId)
            .with(organizer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(published)))
        .andExpect(status().isOk());
  }
}
