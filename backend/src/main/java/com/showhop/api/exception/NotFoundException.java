package com.showhop.api.exception;

/** Base type for "the thing you asked for doesn't exist" failures. */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String message) {
    super(message);
  }
}
