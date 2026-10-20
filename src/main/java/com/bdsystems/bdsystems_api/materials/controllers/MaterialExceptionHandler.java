package com.bdsystems.bdsystems_api.materials.controllers;

import com.bdsystems.bdsystems_api.auth.exceptions.ApiError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = MaterialController.class)
public class MaterialExceptionHandler {
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> conflict(DataIntegrityViolationException exception) {
    return ResponseEntity.status(409).body(new ApiError(409,
      "Material catalog constraint conflict",
      null));
  }
}
