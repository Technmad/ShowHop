package com.showhop.api.exception;

public class WebhookEndpointNotFoundException extends NotFoundException {

  public WebhookEndpointNotFoundException(String message) {
    super(message);
  }
}
