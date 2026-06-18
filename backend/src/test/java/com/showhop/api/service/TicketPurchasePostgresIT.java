package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.showhop.api.entity.Event;
import com.showhop.api.entity.TicketType;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.EventStatus;
import com.showhop.api.entity.enums.TicketStatus;
import com.showhop.api.exception.TicketsSoldOutException;
import com.showhop.api.repository.EventRepository;
import com.showhop.api.repository.TicketRepository;
import com.showhop.api.repository.TicketTypeRepository;
import com.showhop.api.repository.UserRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Re-runs the purchase-lock correctness scenario from
 * {@link TicketPurchaseConcurrencyTest} against the real PostgreSQL
 * instance started by {@code docker compose up -d db}, rather than H2's
 * Postgres-compatibility mode -- proving the Flyway migrations and the
 * PESSIMISTIC_WRITE lock behave the same way on the actual target
 * database. See docs/adr/0001 for why this isn't Testcontainers-managed.
 */
@SpringBootTest
class TicketPurchasePostgresIT {

  private static final int CAPACITY = 3;
  private static final int CONCURRENT_BUYERS = 12;

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

  @Autowired
  private TicketPurchaseService ticketPurchaseService;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private EventRepository eventRepository;
  @Autowired
  private TicketTypeRepository ticketTypeRepository;
  @Autowired
  private TicketRepository ticketRepository;

  @Test
  void migrationsApplyAndThePurchaseLockHoldsAgainstRealPostgres() throws InterruptedException {
    User organizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Organizer").email("org-it-" + UUID.randomUUID() + "@example.com")
        .build());

    Instant now = Instant.now();
    Event event = eventRepository.saveAndFlush(Event.builder()
        .name("Postgres IT Event")
        .venue("Main Arena")
        .startsAt(now.plusSeconds(3600))
        .endsAt(now.plusSeconds(7200))
        .status(EventStatus.PUBLISHED)
        .organizer(organizer)
        .build());

    TicketType ticketType = ticketTypeRepository.saveAndFlush(TicketType.builder()
        .event(event)
        .name("General Admission")
        .price(new BigDecimal("29.99"))
        .totalAvailable(CAPACITY)
        .build());

    List<UUID> buyerIds = new ArrayList<>();
    for (int i = 0; i < CONCURRENT_BUYERS; i++) {
      buyerIds.add(userRepository.saveAndFlush(User.builder()
              .id(UUID.randomUUID()).name("Buyer " + i)
              .email("buyer-it-" + UUID.randomUUID() + "@example.com").build())
          .getId());
    }

    ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_BUYERS);
    CountDownLatch startingGun = new CountDownLatch(1);
    AtomicInteger succeeded = new AtomicInteger();
    AtomicInteger soldOut = new AtomicInteger();

    List<Future<?>> futures = new ArrayList<>();
    for (UUID buyerId : buyerIds) {
      futures.add(pool.submit(() -> {
        try {
          startingGun.await();
          ticketPurchaseService.purchaseTicket(buyerId, event.getId(), ticketType.getId());
          succeeded.incrementAndGet();
        } catch (TicketsSoldOutException e) {
          soldOut.incrementAndGet();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }));
    }

    startingGun.countDown();
    for (Future<?> future : futures) {
      try {
        future.get(60, TimeUnit.SECONDS);
      } catch (Exception e) {
        throw new AssertionError("Purchase task failed unexpectedly", e);
      }
    }
    pool.shutdown();

    assertThat(succeeded.get()).isEqualTo(CAPACITY);
    assertThat(soldOut.get()).isEqualTo(CONCURRENT_BUYERS - CAPACITY);
    assertThat(ticketRepository.countByTicketTypeIdAndStatus(ticketType.getId(), TicketStatus.PURCHASED))
        .isEqualTo(CAPACITY);
  }
}
