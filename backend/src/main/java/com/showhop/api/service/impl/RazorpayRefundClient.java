package com.showhop.api.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.showhop.api.config.RazorpayProperties;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Issues full refunds via Razorpay's Refunds API -- the compensation path
 * for a reservation that's paid but expired past the point capacity can
 * still honor it (PRD &sect;4.2, &sect;7). An empty body refunds the entire
 * captured amount; there's no partial-refund path in this design (&sect;5
 * non-goals).
 */
@Component
public class RazorpayRefundClient {

  private final RestClient restClient;
  private final RazorpayProperties properties;

  public RazorpayRefundClient(@Qualifier("razorpayRestClient") RestClient restClient, RazorpayProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  public RazorpayRefund refundFull(String paymentId) {
    return restClient.post()
        .uri(properties.baseUrl() + "/payments/" + paymentId + "/refund")
        .headers(headers -> headers.setBasicAuth(properties.keyId(), properties.keySecret()))
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of())
        .retrieve()
        .body(RazorpayRefund.class);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RazorpayRefund(String id, String status) {
  }
}
