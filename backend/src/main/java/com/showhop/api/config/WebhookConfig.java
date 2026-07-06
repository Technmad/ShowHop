package com.showhop.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(WebhookProperties.class)
public class WebhookConfig {

  @Bean
  public RestClient webhookRestClient(RestClient.Builder builder) {
    return builder.build();
  }
}
