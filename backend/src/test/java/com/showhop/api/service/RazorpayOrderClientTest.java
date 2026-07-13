package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.showhop.api.config.RazorpayProperties;
import com.showhop.api.service.impl.RazorpayOrderClient;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RazorpayOrderClientTest {

  private MockRestServiceServer mockServer;
  private RazorpayOrderClient client;
  private RazorpayProperties properties;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    properties = new RazorpayProperties(
        "rzp_test_key", "rzp_test_secret", "whsec_test", "https://api.razorpay.com/v1", Duration.ofMinutes(12));
    client = new RazorpayOrderClient(builder.build(), properties);
  }

  @Test
  void createsAnOrderInPaiseWithAutomaticCaptureAndBasicAuth() {
    String expectedAuth = "Basic " + Base64.getEncoder()
        .encodeToString("rzp_test_key:rzp_test_secret".getBytes());

    mockServer.expect(requestTo("https://api.razorpay.com/v1/orders"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", expectedAuth))
        .andRespond(withSuccess("""
            {"id":"order_abc123","amount":299900,"currency":"INR","receipt":"resv-1","status":"created"}
            """, MediaType.APPLICATION_JSON));

    RazorpayOrderClient.RazorpayOrder order = client.createOrder(299900, "resv-1");

    mockServer.verify();
    assertThat(order.id()).isEqualTo("order_abc123");
    assertThat(order.amount()).isEqualTo(299900);
    assertThat(order.currency()).isEqualTo("INR");
  }
}
