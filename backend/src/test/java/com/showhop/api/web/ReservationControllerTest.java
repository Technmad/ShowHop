package com.showhop.api.web;

import static com.showhop.api.testsupport.JwtTestSupport.authenticatedAs;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showhop.api.dto.EventRequestDto;
import com.showhop.api.dto.ReservationRequestDto;
import com.showhop.api.dto.TicketTypeRequestDto;
import com.showhop.api.entity.enums.EventStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * Overrides the razorpayRestClient bean with one bound to
 * MockRestServiceServer -- the real RazorpayOrderClient runs unmocked
 * against it -- rather than mocking RazorpayOrderClient itself, which
 * fails under this environment's JDK/Mockito combination (the same
 * "cannot mock this class" limitation ReservationServiceImplTest works
 * around). {@code spring.main.allow-bean-definition-overriding=true} lets
 * RazorpayTestConfig's bean, sharing the same bean name, replace the
 * production one from RazorpayConfig.
 */
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@Import(ReservationControllerTest.RazorpayTestConfig.class)
@Transactional
class ReservationControllerTest {

  @TestConfiguration
  static class RazorpayTestConfig {
    static MockRestServiceServer mockServer;

    @Bean
    RestClient razorpayRestClient(RestClient.Builder builder) {
      mockServer = MockRestServiceServer.bindTo(builder).build();
      return builder.build();
    }
  }

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void stubRazorpayOrderCreation() {
    RazorpayTestConfig.mockServer.reset();
    RazorpayTestConfig.mockServer.expect(ExpectedCount.manyTimes(), requestTo("https://api.razorpay.com/v1/orders"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("""
            {"id":"order_abc123","amount":29900,"currency":"INR","receipt":"receipt"}
            """, MediaType.APPLICATION_JSON));
  }

  @Test
  void reservesInventoryAndReturnsRazorpayOrderDetailsForCheckout() throws Exception {
    RequestPostProcessor organizer = authenticatedAs("ORGANIZER");
    RequestPostProcessor buyer = authenticatedAs("ATTENDEE");
    UUID eventId = createPublishedEvent(organizer);
    UUID ticketTypeId = createTicketType(organizer, eventId, new BigDecimal("299.00"), 5);

    mockMvc.perform(post(reservationsPath(eventId, ticketTypeId))
            .with(buyer)
            .header("Idempotency-Key", "idem-" + UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new ReservationRequestDto(1))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.state").value("HELD"))
        .andExpect(jsonPath("$.razorpayOrderId").value("order_abc123"))
        .andExpect(jsonPath("$.razorpayKeyId").isNotEmpty())
        .andExpect(jsonPath("$.amount").value(29900));
  }

  @Test
  void aSecondReservationIsRejectedOnceTheOneAvailableSlotIsHeld() throws Exception {
    RequestPostProcessor organizer = authenticatedAs("ORGANIZER");
    RequestPostProcessor buyerOne = authenticatedAs("ATTENDEE");
    RequestPostProcessor buyerTwo = authenticatedAs("ATTENDEE");
    UUID eventId = createPublishedEvent(organizer);
    UUID ticketTypeId = createTicketType(organizer, eventId, new BigDecimal("299.00"), 1);
    String path = reservationsPath(eventId, ticketTypeId);

    mockMvc.perform(post(path)
            .with(buyerOne)
            .header("Idempotency-Key", "idem-" + UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new ReservationRequestDto(1))))
        .andExpect(status().isCreated());

    mockMvc.perform(post(path)
            .with(buyerTwo)
            .header("Idempotency-Key", "idem-" + UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new ReservationRequestDto(1))))
        .andExpect(status().isConflict());
  }

  @Test
  void aRetriedRequestWithTheSameIdempotencyKeyReturnsTheSameReservation() throws Exception {
    RequestPostProcessor organizer = authenticatedAs("ORGANIZER");
    RequestPostProcessor buyer = authenticatedAs("ATTENDEE");
    UUID eventId = createPublishedEvent(organizer);
    UUID ticketTypeId = createTicketType(organizer, eventId, new BigDecimal("299.00"), 5);
    String path = reservationsPath(eventId, ticketTypeId);
    String idempotencyKey = "idem-" + UUID.randomUUID();

    String firstResponse = mockMvc.perform(post(path)
            .with(buyer)
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new ReservationRequestDto(1))))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    String firstId = objectMapper.readTree(firstResponse).get("id").asText();

    String secondResponse = mockMvc.perform(post(path)
            .with(buyer)
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new ReservationRequestDto(1))))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String secondId = objectMapper.readTree(secondResponse).get("id").asText();
    org.assertj.core.api.Assertions.assertThat(secondId).isEqualTo(firstId);
  }

  @Test
  void aBuyerCanPollTheirOwnReservationStatusButNotAnotherBuyersReservation() throws Exception {
    RequestPostProcessor organizer = authenticatedAs("ORGANIZER");
    UUID owningBuyerId = UUID.randomUUID();
    RequestPostProcessor owningBuyer = authenticatedAs("ATTENDEE", owningBuyerId);
    RequestPostProcessor otherBuyer = authenticatedAs("ATTENDEE");
    UUID eventId = createPublishedEvent(organizer);
    UUID ticketTypeId = createTicketType(organizer, eventId, new BigDecimal("299.00"), 5);

    String createResponse = mockMvc.perform(post(reservationsPath(eventId, ticketTypeId))
            .with(owningBuyer)
            .header("Idempotency-Key", "idem-" + UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new ReservationRequestDto(1))))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    String reservationId = objectMapper.readTree(createResponse).get("id").asText();

    mockMvc.perform(get("/api/v1/reservations/" + reservationId).with(owningBuyer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("HELD"));

    mockMvc.perform(get("/api/v1/reservations/" + reservationId).with(otherBuyer))
        .andExpect(status().isNotFound());
  }

  private String reservationsPath(UUID eventId, UUID ticketTypeId) {
    return "/api/v1/published-events/" + eventId + "/ticket-types/" + ticketTypeId + "/reservations";
  }

  private UUID createPublishedEvent(RequestPostProcessor organizer) throws Exception {
    Instant now = Instant.now();
    EventRequestDto request = new EventRequestDto(
        "Autumn Tech Meetup", "Riverside Hall", now.plusSeconds(3600), now.plusSeconds(7200),
        null, null, EventStatus.PUBLISHED);

    String json = mockMvc.perform(post("/api/v1/events")
            .with(organizer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    return UUID.fromString(objectMapper.readTree(json).get("id").asText());
  }

  private UUID createTicketType(
      RequestPostProcessor organizer, UUID eventId, BigDecimal price, int capacity) throws Exception {
    TicketTypeRequestDto request = new TicketTypeRequestDto("General Admission", null, price, capacity);

    String json = mockMvc.perform(post("/api/v1/events/" + eventId + "/ticket-types")
            .with(organizer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    return UUID.fromString(objectMapper.readTree(json).get("id").asText());
  }
}
