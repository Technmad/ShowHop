package com.showhop.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.entity.User;
import com.showhop.api.entity.WebhookEndpoint;
import com.showhop.api.entity.enums.WebhookEndpointStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WebhookEndpointRepositoryTest {

  @Autowired
  private WebhookEndpointRepository webhookEndpointRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void savesAnEndpointWithJsonSubscribedEventTypes() {
    User organizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Priya").email("priya@example.com").build());

    WebhookEndpoint endpoint = WebhookEndpoint.builder()
        .organizerId(organizer.getId())
        .url("https://example.com/hooks")
        .secret("whsec_test")
        .subscribedEventTypes(List.of("event.published", "ticket.purchased"))
        .build();

    WebhookEndpoint saved = webhookEndpointRepository.saveAndFlush(endpoint);
    var reloaded = webhookEndpointRepository.findById(saved.getId()).orElseThrow();

    assertThat(reloaded.getId()).isNotNull();
    assertThat(reloaded.getStatus()).isEqualTo(WebhookEndpointStatus.ACTIVE);
    assertThat(reloaded.getSubscribedEventTypes())
        .containsExactly("event.published", "ticket.purchased");
    assertThat(reloaded.getConsecutiveFailures()).isZero();
  }

  @Test
  void findsEndpointsByOrganizerAndStatus() {
    User organizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Priya").email("priya2@example.com").build());

    webhookEndpointRepository.saveAndFlush(WebhookEndpoint.builder()
        .organizerId(organizer.getId()).url("https://a.example.com").secret("s1")
        .subscribedEventTypes(List.of("event.published"))
        .status(WebhookEndpointStatus.ACTIVE).build());
    webhookEndpointRepository.saveAndFlush(WebhookEndpoint.builder()
        .organizerId(organizer.getId()).url("https://b.example.com").secret("s2")
        .subscribedEventTypes(List.of("event.published"))
        .status(WebhookEndpointStatus.DISABLED).build());

    List<WebhookEndpoint> active = webhookEndpointRepository
        .findByOrganizerIdAndStatus(organizer.getId(), WebhookEndpointStatus.ACTIVE);

    assertThat(active).extracting(WebhookEndpoint::getUrl).containsExactly("https://a.example.com");
  }
}
