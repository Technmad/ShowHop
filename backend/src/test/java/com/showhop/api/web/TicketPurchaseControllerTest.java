package com.showhop.api.web;

import static com.showhop.api.testsupport.JwtTestSupport.authenticatedAs;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class TicketPurchaseControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void aSecondPurchaseIsRejectedOnceTheOneAvailableTicketIsSold() throws Exception {
    RequestPostProcessor organizer = authenticatedAs("ORGANIZER");
    RequestPostProcessor buyerOne = authenticatedAs("ATTENDEE");
    RequestPostProcessor buyerTwo = authenticatedAs("ATTENDEE");

    UUID eventId = createPublishedEvent(organizer);
    UUID ticketTypeId = createTicketType(organizer, eventId, 1);

    String purchasePath =
        "/api/v1/published-events/" + eventId + "/ticket-types/" + ticketTypeId + "/tickets";

    mockMvc.perform(post(purchasePath).with(buyerOne))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PURCHASED"));

    mockMvc.perform(post(purchasePath).with(buyerTwo))
        .andExpect(status().isConflict());
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

  private UUID createTicketType(RequestPostProcessor organizer, UUID eventId, int capacity)
      throws Exception {
    TicketTypeRequestDto request =
        new TicketTypeRequestDto("General Admission", null, new BigDecimal("29.99"), capacity);

    String json = mockMvc.perform(post("/api/v1/events/" + eventId + "/ticket-types")
            .with(organizer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    return UUID.fromString(objectMapper.readTree(json).get("id").asText());
  }
}
