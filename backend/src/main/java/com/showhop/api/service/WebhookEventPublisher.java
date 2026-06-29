package com.showhop.api.service;

import com.showhop.api.entity.enums.WebhookEventType;
import java.util.Map;
import java.util.UUID;

/**
 * Writes the outbox row for a domain event. Callers invoke this from inside
 * their own {@code @Transactional} method -- deliberately not
 * {@code @Transactional} itself, so the write joins the caller's
 * transaction (the default {@code REQUIRED} propagation) rather than
 * committing separately. That's what makes this a true in-transaction
 * outbox: a rolled-back purchase writes no event, and a committed one is
 * guaranteed to have exactly one.
 */
public interface WebhookEventPublisher {

  void publish(UUID organizerId, WebhookEventType type, Map<String, Object> payload);
}
