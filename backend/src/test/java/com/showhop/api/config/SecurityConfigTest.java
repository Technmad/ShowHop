package com.showhop.api.config;

import static com.showhop.api.testsupport.JwtTestSupport.authenticatedAs;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the authorization rules themselves -- independent of whether a
 * controller exists yet at a given path. A path that's supposed to be
 * blocked must return 401/403 before Spring MVC even tries to dispatch
 * (which would otherwise be a 404 for an unmapped path); a path that's
 * supposed to be open reaches dispatch and 404s for that reason instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void publishedEventBrowsingIsPublic() throws Exception {
    mockMvc.perform(get("/api/v1/published-events"))
        .andExpect(status().isOk()); // no auth needed -- an empty page, not 401/403
  }

  @Test
  void creatingAnEventRequiresAuthentication() throws Exception {
    mockMvc.perform(post("/api/v1/events"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void creatingAnEventIsForbiddenForANonOrganizer() throws Exception {
    mockMvc.perform(post("/api/v1/events").with(authenticatedAs("ATTENDEE")))
        .andExpect(status().isForbidden());
  }

  @Test
  void anOrganizerCanReachTheEventsPathIncludingSubResources() throws Exception {
    mockMvc.perform(get("/api/v1/events/" + UUID.randomUUID()).with(authenticatedAs("ORGANIZER")))
        .andExpect(status().isNotFound()); // matcher lets it through; controller 404s the id
  }

  @Test
  void validatingATicketRequiresStaffRole() throws Exception {
    mockMvc.perform(post("/api/v1/ticket-validations").with(authenticatedAs("ATTENDEE")))
        .andExpect(status().isForbidden());

    mockMvc.perform(post("/api/v1/ticket-validations").with(authenticatedAs("STAFF")))
        .andExpect(status().isBadRequest()); // matcher lets it through; empty body fails validation, not auth
  }

  @Test
  void registeringAWebhookEndpointRequiresOrganizerRole() throws Exception {
    mockMvc.perform(post("/api/v1/webhook-endpoints"))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(post("/api/v1/webhook-endpoints").with(authenticatedAs("ATTENDEE")))
        .andExpect(status().isForbidden());

    mockMvc.perform(post("/api/v1/webhook-endpoints").with(authenticatedAs("ORGANIZER")))
        .andExpect(status().isBadRequest()); // matcher lets it through; empty body fails validation
  }

  @Test
  void creatingAnApiKeyRequiresOrganizerRole() throws Exception {
    mockMvc.perform(post("/api/v1/api-keys"))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(post("/api/v1/api-keys").with(authenticatedAs("ATTENDEE")))
        .andExpect(status().isForbidden());

    mockMvc.perform(post("/api/v1/api-keys").with(authenticatedAs("ORGANIZER")))
        .andExpect(status().isBadRequest()); // matcher lets it through; empty body fails validation
  }

  @Test
  void replayingADeliveryRequiresOrganizerRole() throws Exception {
    mockMvc.perform(post("/api/v1/webhook-deliveries/" + UUID.randomUUID() + "/replay")
            .with(authenticatedAs("STAFF")))
        .andExpect(status().isForbidden());
  }

  @Test
  void purchasingATicketUnderPublishedEventsOnlyRequiresAuthenticationNotAnOrganizerRole()
      throws Exception {
    String purchasePath = "/api/v1/published-events/" + UUID.randomUUID()
        + "/ticket-types/" + UUID.randomUUID() + "/tickets";

    mockMvc.perform(post(purchasePath))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(post(purchasePath).with(authenticatedAs("ATTENDEE")))
        .andExpect(status().isNotFound()); // matcher lets it through; no controller yet
  }

  @Test
  void razorpayWebhookRequiresNoAuthenticationAtAll() throws Exception {
    mockMvc.perform(post("/api/v1/razorpay/webhook"))
        // matcher lets an entirely unauthenticated request through (no
        // 401/403); it 400s only because the required signature header is
        // missing, never because of Spring Security.
        .andExpect(status().isBadRequest());
  }
}
