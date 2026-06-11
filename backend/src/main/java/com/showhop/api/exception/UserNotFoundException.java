package com.showhop.api.exception;

public class UserNotFoundException extends NotFoundException {

  public UserNotFoundException(String message) {
    super(message);
  }
}
