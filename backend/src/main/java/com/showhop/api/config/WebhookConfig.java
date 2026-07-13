package com.showhop.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(WebhookProperties.class)
public class WebhookConfig {

  /**
   * Primary so existing unqualified injection points (e.g.
   * {@code WebhookDeliveryWorker}) keep resolving unambiguously now that
   * {@code RazorpayConfig} registers a second {@link RestClient} bean --
   * that one is only ever injected via its explicit
   * {@code @Qualifier("razorpayRestClient")}.
   */
  @Bean
  @Primary
  public RestClient webhookRestClient(RestClient.Builder builder) {
    return builder.build();
  }
}
