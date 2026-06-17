package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.math.BigDecimal;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * The load-bearing correctness test for this arc: proves the pessimistic
 * lock in {@code TicketPurchaseServiceImpl} actually serializes concurrent
 * purchases against the same ticket type, rather than just looking correct
 * on a single thread. Runs against the real Spring context and a real
 * (H2, Postgres-compat) connection pool -- multiple genuine JDBC
 * connections and transactions racing each other, not a mocked repository.
 */
@SpringBootTest
class TicketPurchaseConcurrencyTest {

  private static final int CAPACITY = 5;
  private static final int CONCURRENT_BUYERS = 20;

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
  void neverSellsMoreTicketsThanCapacityUnderConcurrentPurchases() throws InterruptedException {
    User organizer = userRepository.saveAndFlush(
        User.builder().id(UUID.randomUUID()).name("Organizer").email("org@example.com").build());

    Instant now = Instant.now();
    Event event = eventRepository.saveAndFlush(Event.builder()
        .name("Hot On-Sale Event")
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
      User buyer = userRepository.saveAndFlush(User.builder()
          .id(UUID.randomUUID()).name("Buyer " + i).email("buyer" + i + "@example.com").build());
      buyerIds.add(buyer.getId());
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

    startingGun.countDown(); // release all threads at once
    for (Future<?> future : futures) {
      try {
        future.get(30, TimeUnit.SECONDS);
      } catch (Exception e) {
        throw new AssertionError("Purchase task failed unexpectedly", e);
      }
    }
    pool.shutdown();

    assertThat(succeeded.get()).isEqualTo(CAPACITY);
    assertThat(soldOut.get()).isEqualTo(CONCURRENT_BUYERS - CAPACITY);

    int actuallyPersisted =
        ticketRepository.countByTicketTypeIdAndStatus(ticketType.getId(), TicketStatus.PURCHASED);
    assertThat(actuallyPersisted).isEqualTo(CAPACITY);
  }
}
