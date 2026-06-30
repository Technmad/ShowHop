package com.showhop.api.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * HMAC-SHA256 signing for outbound webhook deliveries, following the same
 * "timestamp.payload" convention Stripe/Svix/GitHub use: the receiver
 * recomputes the signature over the exact bytes sent, keyed by the shared
 * per-endpoint secret, and rejects timestamps outside its own tolerance
 * window. Reused verbatim for inbound Razorpay webhook verification once
 * Phase 3 (payments) lands -- one signing story, two directions.
 */
@Component
public class WebhookSigner {

  private static final String ALGORITHM = "HmacSHA256";

  public String sign(String secret, String timestamp, String payload) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
      byte[] signed = mac.doFinal(
          (timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
      return toHex(signed);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("Unable to compute webhook signature", e);
    }
  }

  private String toHex(byte[] bytes) {
    StringBuilder hex = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }
}
