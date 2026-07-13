package com.showhop.api.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(RazorpayProperties.class)
public class RazorpayConfig {

  @Bean
  @Qualifier("razorpayRestClient")
  public RestClient razorpayRestClient(RestClient.Builder builder) {
    return builder.build();
  }
}
