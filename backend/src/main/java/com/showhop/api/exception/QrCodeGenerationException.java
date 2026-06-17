package com.showhop.api.exception;

/** Wraps ZXing's checked exceptions -- a QR encoding failure is a bug, not something callers recover from. */
public class QrCodeGenerationException extends RuntimeException {

  public QrCodeGenerationException(String message, Throwable cause) {
    super(message, cause);
  }
}
