package com.showhop.api.service.impl;

import com.showhop.api.config.RazorpayProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Verifies the {@code X-Razorpay-Signature} header on inbound webhook
 * requests (PRD &sect;4.2). Razorpay signs the raw request body directly
 * with HMAC-SHA256 keyed by the dashboard-configured webhook secret -- no
 * "timestamp.payload" prefix, unlike the outbound engine's own convention
 * (&sect;4.1), so this recomputes over the body alone via
 * {@link WebhookSigner#hmacHex}, the same underlying primitive.
 *
 * <p>The caller must pass the *exact* bytes Razorpay signed -- read as a raw
 * string, never a re-serialized object -- or verification will fail even
 * for a genuine event.
 */
@Component
@RequiredArgsConstructor
public class RazorpaySignatureVerifier {

  private final WebhookSigner webhookSigner;
  private final RazorpayProperties properties;

  public boolean isValid(String rawBody, String signatureHeader) {
    if (signatureHeader == null) {
      return false;
    }
    String expected = webhookSigner.hmacHex(properties.webhookSecret(), rawBody);
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8),
        signatureHeader.getBytes(StandardCharsets.UTF_8));
  }
}
