package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.service.impl.WebhookSigner;
import org.junit.jupiter.api.Test;

class WebhookSignerTest {

  private final WebhookSigner signer = new WebhookSigner();

  @Test
  void producesTheSameSignatureForTheSameInputs() {
    String a = signer.sign("whsec_test", "1700000000", "{\"type\":\"event.published\"}");
    String b = signer.sign("whsec_test", "1700000000", "{\"type\":\"event.published\"}");

    assertThat(a).isEqualTo(b);
    assertThat(a).matches("[0-9a-f]{64}"); // hex-encoded SHA-256 digest
  }

  @Test
  void differentSecretsProduceDifferentSignatures() {
    String a = signer.sign("whsec_one", "1700000000", "payload");
    String b = signer.sign("whsec_two", "1700000000", "payload");

    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void differentTimestampsProduceDifferentSignatures() {
    String a = signer.sign("whsec_test", "1700000000", "payload");
    String b = signer.sign("whsec_test", "1700000001", "payload");

    assertThat(a).isNotEqualTo(b);
  }
}
