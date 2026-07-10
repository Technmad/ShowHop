package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.showhop.api.entity.User;
import com.showhop.api.entity.WebhookDelivery;
import com.showhop.api.entity.WebhookEndpoint;
import com.showhop.api.entity.WebhookEvent;
import com.showhop.api.entity.enums.WebhookDeliveryState;
import com.showhop.api.entity.enums.WebhookEndpointStatus;
import com.showhop.api.entity.enums.WebhookEventType;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.repository.WebhookDeliveryRepository;
import com.showhop.api.repository.WebhookEndpointRepository;
import com.showhop.api.repository.WebhookEventRepository;
import com.showhop.api.service.impl.WebhookDeliveryWorker;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Proves the SKIP LOCKED claim query is actually safe for competing
 * consumers -- against real PostgreSQL, since H2 doesn't reliably model
 * SKIP LOCKED semantics under real concurrency. Same pattern as
 * {@code TicketPurchasePostgresIT}: {@code docker compose up -d db} to
 * include this test (docs/adr/0001).
 */
@SpringBootTest
class WebhookDeliveryClaimPostgresIT {

  private static final int TOTAL_DELIVERIES = 30;
  private static final int CONCURRENT_WORKERS = 6;

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/postgres");
    registry.add("spring.datasource.username", () -> "postgres");
    registry.add("spring.datasource.password", () -> "changeme-local-only");
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
  }

  @BeforeAll
  static void requireLocalPostgres() {
    assumeTrue(portIsOpen("localhost", 5432),
        "Skipping: no Postgres on localhost:5432. Run `docker compose up -d db` "
            + "to include this test (see docs/adr/0001).");
  }

  private static boolean portIsOpen(String host, int port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), 500);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  @Autowired private WebhookDeliveryWorker worker;
  @Autowired private WebhookDeliveryRepository webhookDeliveryRepository;
  @Autowired private WebhookEndpointRepository webhookEndpointRepository;
  @Autowired private WebhookEventRepository webhookEventRepository;
  @Autowired private UserRepository userRepository;

  @Test
  void concurrentWorkersNeverClaimTheSameDeliveryTwice() throws Exception {
    User organizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Organizer")
        .email("wh-claim-it-" + UUID.randomUUID() + "@example.com").build());
    WebhookEndpoint endpoint = webhookEndpointRepository.saveAndFlush(WebhookEndpoint.builder()
        .organizerId(organizer.getId()).url("https://example.com/hooks").secret("whsec_it")
        .subscribedEventTypes(List.of("event.published"))
        .status(WebhookEndpointStatus.ACTIVE).build());
    WebhookEvent event = webhookEventRepository.saveAndFlush(WebhookEvent.builder()
        .organizerId(organizer.getId()).type(WebhookEventType.EVENT_PUBLISHED)
        .payload(Map.of("eventId", "abc")).occurredAt(Instant.now()).build());

    List<UUID> deliveryIds = new ArrayList<>();
    for (int i = 0; i < TOTAL_DELIVERIES; i++) {
      deliveryIds.add(webhookDeliveryRepository.saveAndFlush(WebhookDelivery.builder()
          .endpoint(endpoint).event(event)
          .state(WebhookDeliveryState.PENDING).attempt(0).maxAttempts(8).build())
          .getId());
    }

    ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_WORKERS);
    CountDownLatch startingGun = new CountDownLatch(1);
    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < CONCURRENT_WORKERS; i++) {
      futures.add(pool.submit(() -> {
        startingGun.await();
        // Mirrors how WebhookScheduler actually drives this: repeated
        // polling ticks, not a single call. A single simultaneous burst
        // of SELECT ... LIMIT 20 FOR UPDATE SKIP LOCKED across 6 workers
        // isn't guaranteed to divide 30 rows evenly in one round -- a
        // worker whose query snapshot lands early may see (and SKIP LOCK)
        // rows another worker hasn't reached in its own scan yet, so some
        // rows legitimately go unclaimed on that round and are picked up
        // on the next one. What must never happen, at any round, is two
        // workers claiming the same row -- that's what this test proves.
        for (int round = 0; round < 20 && !worker.claimBatch().isEmpty(); round++) {
          // keep polling until this worker finds nothing left to claim
        }
        return null;
      }));
    }
    startingGun.countDown();
    for (Future<?> future : futures) {
      future.get(60, TimeUnit.SECONDS);
    }
    pool.shutdown();

    List<WebhookDelivery> claimed = webhookDeliveryRepository.findAllById(deliveryIds);

    // Every delivery was claimed exactly once by exactly one worker -- if
    // SKIP LOCKED had let two workers claim the same row, its attempt
    // count would be 2 (incremented once per claim) instead of 1.
    assertThat(claimed).hasSize(TOTAL_DELIVERIES);
    assertThat(claimed).allSatisfy(delivery -> {
      assertThat(delivery.getState()).isEqualTo(WebhookDeliveryState.IN_FLIGHT);
      assertThat(delivery.getAttempt()).isEqualTo(1);
      assertThat(delivery.getLockedBy()).isNotNull();
    });
  }
}
