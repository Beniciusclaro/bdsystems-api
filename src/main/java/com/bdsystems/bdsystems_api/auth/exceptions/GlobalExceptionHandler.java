package com.bdsystems.bdsystems_api.auth.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(EmailAlreadyRegisteredException.class)
  public ResponseEntity<ApiError> handleEmailAlreadyRegistered(
      EmailAlreadyRegisteredException exception
  ) {
    ApiError error = new ApiError(
        HttpStatus.CONFLICT.value(),
        exception.getMessage(),
        "email"
    );

    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(error);
  }
}
