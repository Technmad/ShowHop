package com.showhop.api.web;

import static com.showhop.api.testsupport.JwtTestSupport.authenticatedAs;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showhop.api.dto.WebhookEndpointPatchDto;
import com.showhop.api.dto.WebhookEndpointRequestDto;
import java.util.List;
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
class WebhookEndpointControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void registeringAnEndpointReturnsItsSecretOnlyOnce() throws Exception {
    RequestPostProcessor organizer = authenticatedAs("ORGANIZER");
    WebhookEndpointRequestDto request =
        new WebhookEndpointRequestDto("https://example.com/hooks", List.of("event.published"));

    String createdJson = mockMvc.perform(post("/api/v1/webhook-endpoints")
            .with(organizer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.url").value("https://example.com/hooks"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.secret").exists())
        .andReturn().getResponse().getContentAsString();
    UUID endpointId = UUID.fromString(objectMapper.readTree(createdJson).get("id").asText());

    mockMvc.perform(get("/api/v1/webhook-endpoints").with(organizer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].secret").doesNotExist());

    mockMvc.perform(patch("/api/v1/webhook-endpoints/" + endpointId)
            .with(organizer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new WebhookEndpointPatchDto(false, null, null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DISABLED"))
        .andExpect(jsonPath("$.secret").doesNotExist());
  }

  @Test
  void aSecretRotationExposesTheNewSecretButOnlyThatOnce() throws Exception {
    RequestPostProcessor organizer = authenticatedAs("ORGANIZER");
    String createdJson = mockMvc.perform(post("/api/v1/webhook-endpoints")
            .with(organizer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new WebhookEndpointRequestDto("https://example.com/hooks", List.of("event.published")))))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    UUID endpointId = UUID.fromString(objectMapper.readTree(createdJson).get("id").asText());
    String originalSecret = objectMapper.readTree(createdJson).get("secret").asText();

    String rotatedJson = mockMvc.perform(patch("/api/v1/webhook-endpoints/" + endpointId)
            .with(organizer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new WebhookEndpointPatchDto(null, null, true))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.secret").exists())
        .andReturn().getResponse().getContentAsString();
    String rotatedSecret = objectMapper.readTree(rotatedJson).get("secret").asText();

    org.assertj.core.api.Assertions.assertThat(rotatedSecret).isNotEqualTo(originalSecret);
  }

  @Test
  void anOrganizerCannotPatchAnotherOrganizersEndpoint() throws Exception {
    RequestPostProcessor owner = authenticatedAs("ORGANIZER");
    RequestPostProcessor someoneElse = authenticatedAs("ORGANIZER");
    String createdJson = mockMvc.perform(post("/api/v1/webhook-endpoints")
            .with(owner)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new WebhookEndpointRequestDto("https://example.com/hooks", List.of("event.published")))))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    UUID endpointId = UUID.fromString(objectMapper.readTree(createdJson).get("id").asText());

    mockMvc.perform(patch("/api/v1/webhook-endpoints/" + endpointId)
            .with(someoneElse)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new WebhookEndpointPatchDto(false, null, null))))
        .andExpect(status().isNotFound());
  }
}
