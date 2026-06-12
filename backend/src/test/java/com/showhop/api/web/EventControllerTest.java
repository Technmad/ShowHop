package com.showhop.api.web;

import static com.showhop.api.testsupport.JwtTestSupport.authenticatedAs;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showhop.api.dto.EventRequestDto;
import com.showhop.api.entity.enums.EventStatus;
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
class EventControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void anOrganizerCanCreateReadUpdateAndDeleteTheirOwnEvent() throws Exception {
    RequestPostProcessor organizer = authenticatedAs("ORGANIZER");

    String createBody = objectMapper.writeValueAsString(anEventRequest());

    String createdJson = mockMvc.perform(post("/api/v1/events")
            .with(organizer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(createBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Autumn Tech Meetup"))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andReturn().getResponse().getContentAsString();

    UUID eventId = UUID.fromString(objectMapper.readTree(createdJson).get("id").asText());

    mockMvc.perform(get("/api/v1/events/" + eventId).with(organizer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.venue").value("Riverside Hall"));

    mockMvc.perform(get("/api/v1/events").with(organizer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)));

    EventRequestDto updated = new EventRequestDto(
        "Autumn Tech Meetup (Rescheduled)", "Riverside Hall",
        Instant.now().plusSeconds(7200), Instant.now().plusSeconds(10800),
        null, null, EventStatus.PUBLISHED);

    mockMvc.perform(put("/api/v1/events/" + eventId)
            .with(organizer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updated)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Autumn Tech Meetup (Rescheduled)"))
        .andExpect(jsonPath("$.status").value("PUBLISHED"));

    mockMvc.perform(delete("/api/v1/events/" + eventId).with(organizer))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/v1/events/" + eventId).with(organizer))
        .andExpect(status().isNotFound());
  }

  @Test
  void anOrganizerCannotReadAnotherOrganizersEvent() throws Exception {
    RequestPostProcessor owner = authenticatedAs("ORGANIZER");
    RequestPostProcessor someoneElse = authenticatedAs("ORGANIZER");

    String createdJson = mockMvc.perform(post("/api/v1/events")
            .with(owner)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(anEventRequest())))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    UUID eventId = UUID.fromString(objectMapper.readTree(createdJson).get("id").asText());

    mockMvc.perform(get("/api/v1/events/" + eventId).with(someoneElse))
        .andExpect(status().isNotFound());
  }

  private EventRequestDto anEventRequest() {
    Instant now = Instant.now();
    return new EventRequestDto(
        "Autumn Tech Meetup", "Riverside Hall", now.plusSeconds(3600), now.plusSeconds(7200),
        null, null, EventStatus.DRAFT);
  }

}
