package com.showhop.api.web;

import com.showhop.api.service.RazorpayWebhookService;
import com.showhop.api.service.impl.RazorpaySignatureVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The first unauthenticated (no JWT) POST endpoint in this app (see
 * SecurityConfig) -- trust is enforced entirely by HMAC signature
 * verification here, not Spring Security's OAuth2 resource-server pipeline,
 * since Razorpay has no bearer token to present. {@code rawBody} is read as
 * a plain {@code String}, never a deserialized object, because signature
 * verification must run over the exact bytes Razorpay signed.
 */
@RestController
@RequestMapping("/api/v1/razorpay/webhook")
@RequiredArgsConstructor
public class RazorpayWebhookController {

  private final RazorpaySignatureVerifier signatureVerifier;
  private final RazorpayWebhookService razorpayWebhookService;

  @PostMapping
  public ResponseEntity<Void> handleWebhook(
      @RequestBody String rawBody,
      @RequestHeader("X-Razorpay-Signature") String signature) {
    if (!signatureVerifier.isValid(rawBody, signature)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    razorpayWebhookService.handle(rawBody);
    return ResponseEntity.ok().build();
  }
}
