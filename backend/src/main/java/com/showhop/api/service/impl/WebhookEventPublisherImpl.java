package com.showhop.api.service.impl;

import com.showhop.api.entity.WebhookEvent;
import com.showhop.api.entity.enums.WebhookEventType;
import com.showhop.api.repository.WebhookEventRepository;
import com.showhop.api.service.WebhookEventPublisher;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebhookEventPublisherImpl implements WebhookEventPublisher {

  private final WebhookEventRepository webhookEventRepository;

  @Override
  public void publish(UUID organizerId, WebhookEventType type, Map<String, Object> payload) {
    webhookEventRepository.save(WebhookEvent.builder()
        .organizerId(organizerId)
        .type(type)
        .payload(payload)
        .occurredAt(Instant.now())
        .build());
  }
}
