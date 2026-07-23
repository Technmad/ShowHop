package com.showhop.api.web;

import static com.showhop.api.testsupport.JwtTestSupport.authenticatedAs;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.showhop.api.entity.User;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.service.AuditLogService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditLogControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private AuditLogService auditLogService;
  @Autowired
  private UserRepository userRepository;

  @Test
  void anOrganizerOnlySeesTheirOwnAuditLogEntriesNewestFirst() throws Exception {
    User organizer = anOrganizer();
    User otherOrganizer = anOrganizer();
    RequestPostProcessor asOrganizer = authenticatedAs("ORGANIZER", organizer.getId());

    auditLogService.record(organizer.getId(), organizer.getId(), "EVENT_DELETED", "Event", "event-1", null);
    auditLogService.record(organizer.getId(), organizer.getId(), "API_KEY_CREATED", "ApiKey", "key-1",
        Map.of("name", "CI"));
    auditLogService.record(otherOrganizer.getId(), otherOrganizer.getId(), "EVENT_DELETED", "Event", "event-2", null);

    mockMvc.perform(get("/api/v1/audit-log").with(asOrganizer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].action").value("API_KEY_CREATED"))
        .andExpect(jsonPath("$.content[1].action").value("EVENT_DELETED"));
  }

  @Test
  void requiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/audit-log"))
        .andExpect(status().isUnauthorized());
  }

  private User anOrganizer() {
    return userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Organizer").email("audit-log-" + UUID.randomUUID() + "@example.com")
        .build());
  }
}
