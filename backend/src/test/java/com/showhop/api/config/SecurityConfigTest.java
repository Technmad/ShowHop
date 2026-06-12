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
        .andExpect(status().isNotFound()); // no controller yet, but not 401/403
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
        .andExpect(status().isNotFound()); // matcher lets it through; no controller yet
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
}
