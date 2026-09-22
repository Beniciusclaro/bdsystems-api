package com.bdsystems.builder_api.auth.controllers;

import com.bdsystems.builder_api.auth.domains.dtos.LoginRequest;
import com.bdsystems.builder_api.auth.domains.dtos.LoginResponse;
import com.bdsystems.builder_api.auth.domains.dtos.UserRequest;
import com.bdsystems.builder_api.auth.domains.dtos.UserResponse;
import com.bdsystems.builder_api.auth.services.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

  private final AuthenticationService service;

  public AuthenticationController(
      AuthenticationService service
  ) {
    this.service = service;
  }

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(
      @Valid @RequestBody UserRequest request
  ) {

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(service.register(request));
  }

  @PostMapping("/login")
  public LoginResponse login(@RequestBody LoginRequest request) {
    return service.login(request);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout() {
    System.out.println("Logout endpoint called");
  }
}
