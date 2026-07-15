package com.showhop.api.service;

/**
 * Handles a verified inbound Razorpay webhook request (PRD &sect;4.2). The
 * controller verifies the signature before calling this -- by the time
 * {@link #handle} runs, the raw body is trusted to have actually come from
 * Razorpay.
 */
public interface RazorpayWebhookService {

  /** Idempotent: Razorpay retries on any non-2xx response, so a redelivered event must be a no-op. */
  void handle(String rawBody);
}
