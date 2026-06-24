package com.showhop.api.web;

import static com.showhop.api.testsupport.JwtTestSupport.authenticatedAs;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void aBuyerCanFetchTheirOwnTicketsQrCodeButNoOneElseCan() throws Exception {
    RequestPostProcessor organizer = authenticatedAs("ORGANIZER");
    RequestPostProcessor buyer = authenticatedAs("ATTENDEE");
    RequestPostProcessor someoneElse = authenticatedAs("ATTENDEE");

    UUID eventId = createPublishedEvent(organizer);
    UUID ticketTypeId = createTicketType(organizer, eventId);

    String purchaseJson = mockMvc.perform(post(
            "/api/v1/published-events/" + eventId + "/ticket-types/" + ticketTypeId + "/tickets")
            .with(buyer))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    UUID ticketId = UUID.fromString(objectMapper.readTree(purchaseJson).get("id").asText());

    mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/qr-codes").with(buyer))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG));

    mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/qr-codes").with(someoneElse))
        .andExpect(status().isNotFound());

    mockMvc.perform(get("/api/v1/tickets/" + ticketId).with(buyer))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
            .jsonPath("$.status").value("PURCHASED"));
    mockMvc.perform(get("/api/v1/tickets/" + ticketId).with(someoneElse))
        .andExpect(status().isNotFound());

    mockMvc.perform(get("/api/v1/tickets").with(buyer))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
            .jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)));
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
}
