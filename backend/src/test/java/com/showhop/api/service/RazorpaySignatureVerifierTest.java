package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.config.RazorpayProperties;
import com.showhop.api.service.impl.RazorpaySignatureVerifier;
import com.showhop.api.service.impl.WebhookSigner;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RazorpaySignatureVerifierTest {

  private final WebhookSigner webhookSigner = new WebhookSigner();
  private final RazorpayProperties properties = new RazorpayProperties(
      "rzp_test_key", "rzp_test_secret", "whsec_razorpay_test",
      "https://api.razorpay.com/v1", Duration.ofMinutes(12));
  private final RazorpaySignatureVerifier verifier = new RazorpaySignatureVerifier(webhookSigner, properties);

  @Test
  void acceptsASignatureRecomputedOverTheRawBodyAlone() {
    String rawBody = "{\"event\":\"payment.captured\",\"payload\":{}}";
    String signature = webhookSigner.hmacHex(properties.webhookSecret(), rawBody);

    assertThat(verifier.isValid(rawBody, signature)).isTrue();
  }

  @Test
  void rejectsASignatureComputedWithTheWrongSecret() {
    String rawBody = "{\"event\":\"payment.captured\",\"payload\":{}}";
    String signature = webhookSigner.hmacHex("some_other_secret", rawBody);

    assertThat(verifier.isValid(rawBody, signature)).isFalse();
  }

  @Test
  void rejectsASignatureComputedOverADifferentBody() {
    String signedBody = "{\"event\":\"payment.captured\"}";
    String tamperedBody = "{\"event\":\"payment.failed\"}";
    String signature = webhookSigner.hmacHex(properties.webhookSecret(), signedBody);

    assertThat(verifier.isValid(tamperedBody, signature)).isFalse();
  }

  @Test
  void rejectsAMissingSignatureHeader() {
    assertThat(verifier.isValid("{\"event\":\"payment.captured\"}", null)).isFalse();
  }
}
