package com.showhop.api.exception;

import com.showhop.api.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponseDto> handleNotFound(
      NotFoundException ex, HttpServletRequest request) {
    return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  @ExceptionHandler(TicketsSoldOutException.class)
  public ResponseEntity<ErrorResponseDto> handleSoldOut(
      TicketsSoldOutException ex, HttpServletRequest request) {
    return errorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
  }

  private ResponseEntity<ErrorResponseDto> errorResponse(
      HttpStatus status, String message, HttpServletRequest request) {
    ErrorResponseDto body = new ErrorResponseDto(
        Instant.now(), status.value(), status.getReasonPhrase(), message,
        request.getRequestURI());
    return ResponseEntity.status(status).body(body);
  }
}
