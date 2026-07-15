package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.showhop.api.config.RazorpayProperties;
import com.showhop.api.service.impl.RazorpayRefundClient;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RazorpayRefundClientTest {

  private MockRestServiceServer mockServer;
  private RazorpayRefundClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    RazorpayProperties properties = new RazorpayProperties(
        "rzp_test_key", "rzp_test_secret", "whsec_test", "https://api.razorpay.com/v1", Duration.ofMinutes(12), 100);
    client = new RazorpayRefundClient(builder.build(), properties);
  }

  @Test
  void issuesAFullRefundForTheGivenPayment() {
    mockServer.expect(requestTo("https://api.razorpay.com/v1/payments/pay_xyz789/refund"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("""
            {"id":"rfnd_def456","status":"processed"}
            """, MediaType.APPLICATION_JSON));

    RazorpayRefundClient.RazorpayRefund refund = client.refundFull("pay_xyz789");

    mockServer.verify();
    assertThat(refund.id()).isEqualTo("rfnd_def456");
    assertThat(refund.status()).isEqualTo("processed");
  }
}
