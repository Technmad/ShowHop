package com.showhop.api.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.showhop.api.config.RazorpayProperties;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Creates Razorpay Orders (PRD &sect;4.2) -- amount in paise (the smallest
 * currency unit, matching Razorpay's contract and avoiding floating-point
 * money bugs), {@code payment_capture: 1} for automatic capture so there's
 * no separate manual-capture step in the reservation saga, INR-only. Built
 * on an injectable {@link RestClient} so tests bind
 * {@code MockRestServiceServer} instead of hitting Razorpay's sandbox,
 * mirroring {@code WebhookDeliveryWorker}.
 */
@Component
public class RazorpayOrderClient {

  private final RestClient restClient;
  private final RazorpayProperties properties;

  public RazorpayOrderClient(@Qualifier("razorpayRestClient") RestClient restClient, RazorpayProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  public RazorpayOrder createOrder(long amountInPaise, String receipt) {
    Map<String, Object> body = Map.of(
        "amount", amountInPaise,
        "currency", "INR",
        "receipt", receipt,
        "payment_capture", 1);

    return restClient.post()
        .uri(properties.baseUrl() + "/orders")
        .headers(headers -> headers.setBasicAuth(properties.keyId(), properties.keySecret()))
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(RazorpayOrder.class);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RazorpayOrder(String id, long amount, String currency, String receipt) {
  }
}
