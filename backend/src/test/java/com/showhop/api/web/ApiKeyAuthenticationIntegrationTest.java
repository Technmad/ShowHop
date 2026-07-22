package com.showhop.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.showhop.api.dto.ApiKeyRequestDto;
import com.showhop.api.entity.User;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.service.ApiKeyService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proves the alternate credential end to end: a real, Spring-managed
 * {@code ApiKeyAuthenticationFilter} authenticates the webhook-management
 * API with no JWT at all -- see PRD &sect;4.4 ("programmatic access ...
 * separate from a user's Keycloak session").
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiKeyAuthenticationIntegrationTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ApiKeyService apiKeyService;
  @Autowired
  private UserRepository userRepository;

  @Test
  void aValidApiKeyAuthenticatesAWebhookEndpointsRequestWithNoJwtAtAll() throws Exception {
    User organizer = anOrganizer();
    var created = apiKeyService.createKey(organizer.getId(), new ApiKeyRequestDto("CI integration"));

    mockMvc.perform(get("/api/v1/webhook-endpoints").header("X-Api-Key", created.rawKey()))
        .andExpect(status().isOk());
  }

  @Test
  void aRevokedApiKeyIsRejected() throws Exception {
    User organizer = anOrganizer();
    var created = apiKeyService.createKey(organizer.getId(), new ApiKeyRequestDto("CI integration"));
    apiKeyService.revokeKey(organizer.getId(), created.apiKey().getId());

    mockMvc.perform(get("/api/v1/webhook-endpoints").header("X-Api-Key", created.rawKey()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void aKeyThatWasNeverIssuedIsRejected() throws Exception {
    mockMvc.perform(get("/api/v1/webhook-endpoints").header("X-Api-Key", "shk_totally-bogus-key"))
        .andExpect(status().isUnauthorized());
  }

  private User anOrganizer() {
    return userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Organizer").email("apikey-auth-" + UUID.randomUUID() + "@example.com")
        .build());
  }
}
