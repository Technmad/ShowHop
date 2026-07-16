package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.showhop.api.config.RazorpayProperties;
import com.showhop.api.entity.Event;
import com.showhop.api.entity.TicketReservation;
import com.showhop.api.entity.TicketType;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.EventStatus;
import com.showhop.api.entity.enums.ReservationState;
import com.showhop.api.exception.TicketsSoldOutException;
import com.showhop.api.repository.EventRepository;
import com.showhop.api.repository.TicketReservationRepository;
import com.showhop.api.repository.TicketTypeRepository;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.service.impl.RazorpayOrderClient;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

/**
 * Re-runs the purchase-lock correctness scenario from
 * {@code TicketPurchasePostgresIT}/{@code TicketPurchaseConcurrencyTest} for
 * the reservation path (PRD &sect;4.2, &sect;6 "Concurrent reservation
 * correctness") against real PostgreSQL, plus the expiry-releases-inventory
 * guarantee the synchronous path never had to make. See docs/adr/0001 for
 * why this isn't Testcontainers-managed.
 */
@SpringBootTest
@Import(ReservationClaimPostgresIT.StubRazorpayConfig.class)
class ReservationClaimPostgresIT {

  private static final int CAPACITY = 3;
  private static final int CONCURRENT_BUYERS = 12;

  /**
   * A plain subclass override, not a Mockito mock -- overriding
   * {@code createOrder} in a real, unmocked instance avoids both a real
   * network call and the "cannot mock this class" limitation
   * {@code ReservationServiceImplTest} works around a different way.
   * {@code @Primary} lets it win over the real component-scanned bean
   * without any bean-name collision.
   */
  @TestConfiguration
  static class StubRazorpayConfig {
    @Bean
    @Primary
    RazorpayOrderClient testRazorpayOrderClient(RazorpayProperties properties) {
      return new RazorpayOrderClient(RestClient.builder().build(), properties) {
        @Override
        public RazorpayOrder createOrder(long amountInPaise, String receipt) {
          return new RazorpayOrder("order_" + receipt, amountInPaise, "INR", receipt);
        }
      };
    }
  }

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

  @Autowired private ReservationService reservationService;
  @Autowired private UserRepository userRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private TicketTypeRepository ticketTypeRepository;
  @Autowired private TicketReservationRepository ticketReservationRepository;

  @Test
  void migrationsApplyAndTheReservationLockHoldsAgainstRealPostgres() throws InterruptedException {
    TicketType ticketType = aTicketType(CAPACITY);

    List<UUID> buyerIds = new ArrayList<>();
    for (int i = 0; i < CONCURRENT_BUYERS; i++) {
      buyerIds.add(userRepository.saveAndFlush(User.builder()
              .id(UUID.randomUUID()).name("Buyer " + i)
              .email("buyer-resv-it-" + UUID.randomUUID() + "@example.com").build())
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
          reservationService.reserve(
              buyerId, ticketType.getEvent().getId(), ticketType.getId(), 1, "idem-" + buyerId);
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
        throw new AssertionError("Reservation task failed unexpectedly", e);
      }
    }
    pool.shutdown();

    assertThat(succeeded.get()).isEqualTo(CAPACITY);
    assertThat(soldOut.get()).isEqualTo(CONCURRENT_BUYERS - CAPACITY);
    assertThat(ticketReservationRepository.countActiveHolds(ticketType.getId())).isEqualTo(CAPACITY);
  }

  @Test
  void anExpiredReservationReleasesInventoryForASubsequentReserve() {
    TicketType ticketType = aTicketType(1);
    User firstBuyer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("First buyer")
        .email("buyer-resv-it-" + UUID.randomUUID() + "@example.com").build());
    User secondBuyer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Second buyer")
        .email("buyer-resv-it-" + UUID.randomUUID() + "@example.com").build());

    // An already-expired HELD reservation, seeded directly (bypassing the
    // reaper) -- countActiveHolds excludes it once expired, so the second
    // buyer's reserve() must see the slot as available again.
    ticketReservationRepository.saveAndFlush(TicketReservation.builder()
        .ticketType(ticketType).buyer(firstBuyer).quantity(1)
        .state(ReservationState.HELD).expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
        .idempotencyKey("idem-" + UUID.randomUUID())
        .build());

    var result = reservationService.reserve(
        secondBuyer.getId(), ticketType.getEvent().getId(), ticketType.getId(), 1,
        "idem-" + secondBuyer.getId());

    assertThat(result.reservation().getState()).isEqualTo(ReservationState.HELD);
    assertThat(ticketReservationRepository.countActiveHolds(ticketType.getId())).isEqualTo(1);
  }

  private TicketType aTicketType(int capacity) {
    User organizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Organizer").email("org-resv-it-" + UUID.randomUUID() + "@example.com")
        .build());

    Instant now = Instant.now();
    Event event = eventRepository.saveAndFlush(Event.builder()
        .name("Postgres Reservation IT Event")
        .venue("Main Arena")
        .startsAt(now.plusSeconds(3600))
        .endsAt(now.plusSeconds(7200))
        .status(EventStatus.PUBLISHED)
        .organizer(organizer)
        .build());

    return ticketTypeRepository.saveAndFlush(TicketType.builder()
        .event(event)
        .name("General Admission")
        .price(new BigDecimal("29.99"))
        .totalAvailable(capacity)
        .build());
  }
}
