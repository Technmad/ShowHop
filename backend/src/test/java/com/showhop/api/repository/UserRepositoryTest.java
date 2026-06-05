package com.showhop.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

  @org.springframework.beans.factory.annotation.Autowired
  private UserRepository userRepository;

  @Test
  void savesAndReloadsAUserWithAuditTimestampsStamped() {
    User user = User.builder()
        .id(UUID.randomUUID())
        .name("Priya Shah")
        .email("priya@example.com")
        .build();

    User saved = userRepository.saveAndFlush(user);

    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();

    User reloaded = userRepository.findById(saved.getId()).orElseThrow();
    assertThat(reloaded.getEmail()).isEqualTo("priya@example.com");
  }
}
