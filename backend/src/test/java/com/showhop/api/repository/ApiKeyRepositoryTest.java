package com.showhop.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.entity.ApiKey;
import com.showhop.api.entity.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApiKeyRepositoryTest {

  @Autowired
  private ApiKeyRepository apiKeyRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void findsByOrganizerAndByIdScopedToOrganizer() {
    User organizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Priya").email("priya-apikey@example.com").build());
    User otherOrganizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Aman").email("aman-apikey@example.com").build());

    ApiKey key = apiKeyRepository.saveAndFlush(ApiKey.builder()
        .organizerId(organizer.getId()).name("CI integration").keyPrefix("abcd1234")
        .hashedKey("hashed").build());

    assertThat(apiKeyRepository.findByOrganizerId(organizer.getId(), Pageable.unpaged()).getContent())
        .containsExactly(key);
    assertThat(apiKeyRepository.findByIdAndOrganizerId(key.getId(), organizer.getId())).isPresent();
    assertThat(apiKeyRepository.findByIdAndOrganizerId(key.getId(), otherOrganizer.getId())).isEmpty();
  }

  @Test
  void findsAnActiveKeyByPrefixButNotARevokedOne() {
    User organizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Priya").email("priya-apikey2@example.com").build());

    apiKeyRepository.saveAndFlush(ApiKey.builder()
        .organizerId(organizer.getId()).name("Active").keyPrefix("prefix01")
        .hashedKey("hashed-active").build());
    apiKeyRepository.saveAndFlush(ApiKey.builder()
        .organizerId(organizer.getId()).name("Revoked").keyPrefix("prefix02")
        .hashedKey("hashed-revoked").revokedAt(Instant.now()).build());

    assertThat(apiKeyRepository.findByKeyPrefixAndRevokedAtIsNull("prefix01")).isPresent();
    assertThat(apiKeyRepository.findByKeyPrefixAndRevokedAtIsNull("prefix02")).isEmpty();
  }
}
