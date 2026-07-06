package com.showhop.api.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The only thing in the webhook engine that actually fires on a timer.
 * Kept separate from {@link WebhookFanOutService} and
 * {@link WebhookDeliveryWorker} so those stay plain, always-present,
 * directly callable beans, and this scheduling trigger can be switched off
 * with a single property -- which the test suite does
 * ({@code showhop.webhooks.scheduling-enabled=false}), so a
 * {@code @SpringBootTest} loading the full context never fires background
 * HTTP sends against whatever fake endpoint URLs a test happened to create.
 */
@Component
@ConditionalOnProperty(
    prefix = "showhop.webhooks", name = "scheduling-enabled",
    havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class WebhookScheduler {

  private final WebhookFanOutService fanOutService;
  private final WebhookDeliveryWorker deliveryWorker;

  @Scheduled(fixedDelayString = "${showhop.webhooks.fan-out-interval-ms:5000}")
  public void fanOut() {
    fanOutService.fanOutDueEvents();
  }

  @Scheduled(fixedDelayString = "${showhop.webhooks.delivery-poll-interval-ms:3000}")
  public void deliver() {
    deliveryWorker.pollAndDeliver();
  }
}
