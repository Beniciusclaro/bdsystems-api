package com.bdsystems.bdsystems_api.auth.controllers;

import com.bdsystems.bdsystems_api.auth.domains.dtos.LoginRequest;
import com.bdsystems.bdsystems_api.auth.domains.dtos.LoginResponse;
import com.bdsystems.bdsystems_api.auth.domains.dtos.UserRequest;
import com.bdsystems.bdsystems_api.auth.domains.entities.User;
import com.bdsystems.bdsystems_api.auth.services.AuthenticationService;
import com.bdsystems.bdsystems_api.auth.services.UserRegistrationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

  private final UserRegistrationService userRegistrationService;
  private final AuthenticationService authenticationService;

  public UserController(UserRegistrationService userRegistrationService, AuthenticationService authenticationService) {
    this.userRegistrationService = userRegistrationService;
    this.authenticationService = authenticationService;
  }

  @PostMapping("/register")
  public User registerUser(@RequestBody UserRequest user) {
    return userRegistrationService.createUser(user);
  }

  @PostMapping("/login")
  public LoginResponse login(@RequestBody LoginRequest request) {
    return authenticationService.login(request);
  }
}
