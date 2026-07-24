package com.showhop.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Proves the actuator health indicators actually reach a real datasource,
 * not just that the {@code HealthEndpoint} bean exists (H2 doesn't
 * exercise the same driver/connection-pool health contributor as
 * PostgreSQL). Queries the {@link HealthEndpoint} bean directly rather
 * than over HTTP -- {@code management.server.port=0} (see
 * src/test/resources/application.properties) means the actual bound port
 * isn't known ahead of time, and the bean itself is the thing under test,
 * not the HTTP transport in front of it.
 */
@SpringBootTest
class ActuatorHealthPostgresIT {

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
  private HealthEndpoint healthEndpoint;

  @Test
  void reportsUpAgainstARealDatabase() {
    assertThat(healthEndpoint.health().getStatus()).isEqualTo(Status.UP);
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
