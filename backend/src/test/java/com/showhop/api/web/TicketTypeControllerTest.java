package com.showhop.api.web;

import static com.showhop.api.testsupport.JwtTestSupport.authenticatedAs;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketTypeControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void anOrganizerCanManageTicketTypesOnTheirOwnEventButNotSomeoneElses() throws Exception {
    RequestPostProcessor owner = authenticatedAs("ORGANIZER");
    RequestPostProcessor someoneElse = authenticatedAs("ORGANIZER");

    UUID eventId = createEvent(owner);

    String createdJson = mockMvc.perform(post("/api/v1/events/" + eventId + "/ticket-types")
            .with(owner)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(aTicketTypeRequest())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("General Admission"))
        .andExpect(jsonPath("$.totalAvailable").value(200))
        .andReturn().getResponse().getContentAsString();
    UUID ticketTypeId = UUID.fromString(objectMapper.readTree(createdJson).get("id").asText());

    mockMvc.perform(get("/api/v1/events/" + eventId + "/ticket-types/" + ticketTypeId)
            .with(owner))
        .andExpect(status().isOk());

    TicketTypeRequestDto updated =
        new TicketTypeRequestDto("General Admission", "Now includes a drink", new BigDecimal("34.99"), 150);
    mockMvc.perform(put("/api/v1/events/" + eventId + "/ticket-types/" + ticketTypeId)
            .with(owner)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updated)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.price").value(34.99))
        .andExpect(jsonPath("$.totalAvailable").value(150));

    // A different organizer can't see or touch it, even knowing the ids.
    mockMvc.perform(get("/api/v1/events/" + eventId + "/ticket-types/" + ticketTypeId)
            .with(someoneElse))
        .andExpect(status().isNotFound());
    mockMvc.perform(post("/api/v1/events/" + eventId + "/ticket-types")
            .with(someoneElse)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(aTicketTypeRequest())))
        .andExpect(status().isNotFound());

    mockMvc.perform(delete("/api/v1/events/" + eventId + "/ticket-types/" + ticketTypeId)
            .with(owner))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/v1/events/" + eventId + "/ticket-types/" + ticketTypeId)
            .with(owner))
        .andExpect(status().isNotFound());
  }

  private UUID createEvent(RequestPostProcessor organizer) throws Exception {
    Instant now = Instant.now();
    EventRequestDto request = new EventRequestDto(
        "Autumn Tech Meetup", "Riverside Hall", now.plusSeconds(3600), now.plusSeconds(7200),
        null, null, EventStatus.DRAFT);

    String json = mockMvc.perform(post("/api/v1/events")
            .with(organizer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    return UUID.fromString(objectMapper.readTree(json).get("id").asText());
  }

  private TicketTypeRequestDto aTicketTypeRequest() {
    return new TicketTypeRequestDto("General Admission", null, new BigDecimal("29.99"), 200);
  }
}
