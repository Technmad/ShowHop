package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.showhop.api.entity.User;
import com.showhop.api.repository.AuditLogEntryRepository;
import com.showhop.api.repository.UserRepository;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Proves the guarantee {@code AuditLogService.record}'s Javadoc claims --
 * an entry only survives if the transaction it was written in actually
 * commits -- against real PostgreSQL rather than by code inspection.
 * Deliberately not {@code @Transactional} itself: each test drives its own
 * {@link TransactionTemplate} so the repository read afterward is a
 * genuinely separate transaction, not a read-your-own-writes check inside
 * one still-open transaction (see ReservationControllerTest's Javadoc on
 * the same pitfall in Phase 3).
 */
@SpringBootTest
class AuditLogTransactionalPostgresIT {

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/postgres");
    registry.add("spring.datasource.username", () -> "postgres");
    registry.add("spring.datasource.password", () -> "changeme-local-only");
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    // This test does nothing concurrent -- a small pool keeps this
    // context's connection footprint from adding to the other
    // Postgres-backed IT contexts' pools toward Postgres's max_connections.
    registry.add("spring.datasource.hikari.maximum-pool-size", () -> "3");
  }

  @BeforeAll
  static void requireLocalPostgres() {
    assumeTrue(portIsOpen("localhost", 5432),
        "Skipping: no Postgres on localhost:5432. Run `docker compose up -d db` "
            + "to include this test (see docs/adr/0001).");
  }

  @Autowired
  private AuditLogService auditLogService;
  @Autowired
  private AuditLogEntryRepository auditLogEntryRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private PlatformTransactionManager transactionManager;

  @Test
  void anEntryIsRolledBackWhenItsSurroundingTransactionAborts() {
    UUID organizerId = anOrganizer();
    String entityId = UUID.randomUUID().toString();
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

    assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
      auditLogService.record(organizerId, organizerId, "TEST_ACTION", "Test", entityId, null);
      throw new IllegalStateException("simulating the mutating action failing after the audit write");
    })).isInstanceOf(IllegalStateException.class);

    assertThat(auditLogEntryRepository.findByOrganizerIdOrderByOccurredAtDesc(organizerId, Pageable.unpaged())
        .getContent()).isEmpty();
  }

  @Test
  void anEntryPersistsWhenItsTransactionCommitsNormally() {
    UUID organizerId = anOrganizer();
    String entityId = UUID.randomUUID().toString();
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

    transactionTemplate.executeWithoutResult(status ->
        auditLogService.record(organizerId, organizerId, "TEST_ACTION", "Test", entityId, null));

    assertThat(auditLogEntryRepository.findByOrganizerIdOrderByOccurredAtDesc(organizerId, Pageable.unpaged())
        .getContent()).hasSize(1);
  }

  private UUID anOrganizer() {
    return userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Organizer").email("audit-tx-" + UUID.randomUUID() + "@example.com")
        .build()).getId();
  }

  private static boolean portIsOpen(String host, int port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), 500);
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
