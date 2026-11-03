package com.bdsystems.bdsystems_api.auth.controllers;

import com.bdsystems.bdsystems_api.auth.domains.dtos.LoginRequest;
import com.bdsystems.bdsystems_api.auth.domains.dtos.LoginResponse;
import com.bdsystems.bdsystems_api.auth.domains.dtos.InvitationAcceptanceRequest;
import com.bdsystems.bdsystems_api.auth.domains.dtos.UserRequest;
import com.bdsystems.bdsystems_api.auth.domains.dtos.UserResponse;
import com.bdsystems.bdsystems_api.auth.services.AuthenticationService;
import com.bdsystems.bdsystems_api.auth.services.AuthenticationResult;
import com.bdsystems.bdsystems_api.auth.services.UserInvitationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

  private final AuthenticationService service;
  private final UserInvitationService invitationService;
  private final boolean refreshCookieSecure;
  private static final String REFRESH_COOKIE = "refresh_token";

  public AuthenticationController(
    AuthenticationService service,
    UserInvitationService invitationService,
    @Value("${security.jwt.refresh-cookie-secure}") boolean refreshCookieSecure
  ) {
    this.service = service;
    this.invitationService = invitationService;
    this.refreshCookieSecure = refreshCookieSecure;
  }

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(
    @Valid @RequestBody UserRequest request
  ) {
    log.info("Received request to register company administrator with email {}",
      request.email());
    AuthenticationResult result = service.register(request);
    return ResponseEntity
      .status(HttpStatus.CREATED)
      .header(HttpHeaders.SET_COOKIE,
        refreshCookie(result).toString())
      .body(new UserResponse(result.accessToken(),
        result.expiresInSeconds()));
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    AuthenticationResult result = service.login(request);
    return ResponseEntity.ok()
      .header(HttpHeaders.SET_COOKIE,
        refreshCookie(result).toString())
      .body(new LoginResponse(result.accessToken(),
        result.expiresInSeconds()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<LoginResponse> refresh(
    @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken
  ) {
    AuthenticationResult result = service.refresh(refreshToken);
    return ResponseEntity.ok(new LoginResponse(result.accessToken(),
      result.expiresInSeconds()));
  }

  @PostMapping("/invitations/accept")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void acceptInvitation(
    @Valid @RequestBody InvitationAcceptanceRequest request
  ) {
    invitationService.accept(request.token(),
      request.password());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<Void> logout(
    @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken
  ) {
    service.logout(refreshToken);
    return ResponseEntity.noContent()
      .header(HttpHeaders.SET_COOKIE,
        clearedRefreshCookie().toString())
      .build();
  }

  private ResponseCookie refreshCookie(AuthenticationResult result) {
    ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(REFRESH_COOKIE,
        result.refreshToken())
      .httpOnly(true)
      .secure(refreshCookieSecure)
      .sameSite("Lax")
      .path("/api/auth");
    if (result.persistentCookie()) {
      Duration remainingDuration = Duration.between(Instant.now(),
        result.refreshExpiresAt());
      cookieBuilder.maxAge(remainingDuration.isNegative() ? Duration.ZERO : remainingDuration);
    }
    return cookieBuilder.build();
  }

  private ResponseCookie clearedRefreshCookie() {
    return ResponseCookie.from(REFRESH_COOKIE,
        "")
      .httpOnly(true)
      .secure(refreshCookieSecure)
      .sameSite("Lax")
      .path("/api/auth")
      .maxAge(Duration.ZERO)
      .build();
  }
}
