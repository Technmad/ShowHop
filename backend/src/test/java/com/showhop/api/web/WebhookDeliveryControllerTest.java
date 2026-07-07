package com.showhop.api.web;

import static com.showhop.api.testsupport.JwtTestSupport.authenticatedAs;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showhop.api.dto.WebhookEndpointRequestDto;
import com.showhop.api.entity.User;
import com.showhop.api.entity.WebhookDelivery;
import com.showhop.api.entity.WebhookEndpoint;
import com.showhop.api.entity.WebhookEvent;
import com.showhop.api.entity.enums.WebhookDeliveryState;
import com.showhop.api.entity.enums.WebhookEndpointStatus;
import com.showhop.api.entity.enums.WebhookEventType;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.repository.WebhookDeliveryRepository;
import com.showhop.api.repository.WebhookEndpointRepository;
import com.showhop.api.repository.WebhookEventRepository;
import com.showhop.api.testsupport.JwtTestSupport;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebhookDeliveryControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private WebhookEndpointRepository webhookEndpointRepository;
  @Autowired private WebhookEventRepository webhookEventRepository;
  @Autowired private WebhookDeliveryRepository webhookDeliveryRepository;

  @Test
  void anOrganizerCanListAndReplayDeliveriesForTheirOwnEndpoint() throws Exception {
    UUID organizerId = UUID.randomUUID();
    userRepository.saveAndFlush(User.builder()
        .id(organizerId).name("Priya").email("priya-" + organizerId + "@example.com").build());
    RequestPostProcessor organizer = JwtTestSupport.authenticatedAs("ORGANIZER", organizerId);

    WebhookEndpoint endpoint = webhookEndpointRepository.saveAndFlush(WebhookEndpoint.builder()
        .organizerId(organizerId).url("https://example.com/hooks").secret("whsec_x")
        .subscribedEventTypes(List.of("event.published")).status(WebhookEndpointStatus.ACTIVE)
        .build());
    WebhookEvent event = webhookEventRepository.saveAndFlush(WebhookEvent.builder()
        .organizerId(organizerId).type(WebhookEventType.EVENT_PUBLISHED)
        .payload(Map.of("eventId", "abc")).occurredAt(Instant.now()).build());
    WebhookDelivery delivery = webhookDeliveryRepository.saveAndFlush(WebhookDelivery.builder()
        .endpoint(endpoint).event(event).state(WebhookDeliveryState.DEAD_LETTER)
        .attempt(8).maxAttempts(8).build());

    mockMvc.perform(get("/api/v1/webhook-endpoints/" + endpoint.getId() + "/deliveries")
            .with(organizer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].state").value("DEAD_LETTER"))
        .andExpect(jsonPath("$.content[0].eventType").value("EVENT_PUBLISHED"));

    mockMvc.perform(post("/api/v1/webhook-deliveries/" + delivery.getId() + "/replay")
            .with(organizer))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.state").value("PENDING"))
        .andExpect(jsonPath("$.attempt").value(0));
  }

  @Test
  void listingDeliveriesForAnEndpointThatDoesNotExistIs404() throws Exception {
    mockMvc.perform(get("/api/v1/webhook-endpoints/" + UUID.randomUUID() + "/deliveries")
            .with(authenticatedAs("ORGANIZER")))
        .andExpect(status().isNotFound());
  }

  @Test
  void anOrganizerCannotReplayAnotherOrganizersDelivery() throws Exception {
    UUID owner = UUID.randomUUID();
    userRepository.saveAndFlush(
        User.builder().id(owner).name("Owner").email("owner-" + UUID.randomUUID() + "@example.com").build());
    WebhookEndpoint endpoint = webhookEndpointRepository.saveAndFlush(WebhookEndpoint.builder()
        .organizerId(owner).url("https://example.com/hooks").secret("whsec_x")
        .subscribedEventTypes(List.of("event.published")).status(WebhookEndpointStatus.ACTIVE)
        .build());
    WebhookEvent event = webhookEventRepository.saveAndFlush(WebhookEvent.builder()
        .organizerId(owner).type(WebhookEventType.EVENT_PUBLISHED)
        .payload(Map.of("eventId", "abc")).occurredAt(Instant.now()).build());
    WebhookDelivery delivery = webhookDeliveryRepository.saveAndFlush(WebhookDelivery.builder()
        .endpoint(endpoint).event(event).state(WebhookDeliveryState.DEAD_LETTER).build());

    mockMvc.perform(post("/api/v1/webhook-deliveries/" + delivery.getId() + "/replay")
            .with(authenticatedAs("ORGANIZER"))) // a different organizer than `owner`
        .andExpect(status().isNotFound());
  }
}
