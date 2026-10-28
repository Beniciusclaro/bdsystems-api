package com.bdsystems.bdsystems_api.auth.controllers;

import com.bdsystems.bdsystems_api.auth.domains.dtos.PasswordResetConfirmationRequest;
import com.bdsystems.bdsystems_api.auth.domains.dtos.PasswordResetRequest;
import com.bdsystems.bdsystems_api.auth.services.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {

  private final PasswordResetService passwordResetService;

  public PasswordResetController(PasswordResetService passwordResetService) {
    this.passwordResetService = passwordResetService;
  }

  @PostMapping("/request")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void request(@Valid @RequestBody PasswordResetRequest request) {
    passwordResetService.request(request.email());
  }

  @PostMapping("/confirm")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void confirm(@Valid @RequestBody PasswordResetConfirmationRequest request) {
    passwordResetService.confirm(request.token(),
      request.password());
  }
}
