package com.bdsystems.bdsystems_api.auth.services;

import com.bdsystems.bdsystems_api.auth.domains.entities.RefreshSession;
import com.bdsystems.bdsystems_api.auth.domains.entities.User;
import com.bdsystems.bdsystems_api.auth.repositories.RefreshSessionRepository;
import com.bdsystems.bdsystems_api.employee.domains.entities.EmployeeStatus;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshSessionService {
  private static final int TOKEN_BYTES = 32;

  private final RefreshSessionRepository refreshSessionRepository;
  private final JwtService jwtService;
  private final Duration refreshSessionDuration;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Clock clock = Clock.systemUTC();

  public RefreshSessionService(
    RefreshSessionRepository refreshSessionRepository,
    JwtService jwtService,
    @Value("${security.jwt.refresh-session-duration}") Duration refreshSessionDuration
  ) {
    this.refreshSessionRepository = refreshSessionRepository;
    this.jwtService = jwtService;
    this.refreshSessionDuration = refreshSessionDuration;
  }

  public AuthenticationResult create(User user,
    boolean persistentCookie) {
    assertAccountActive(user);
    Instant createdAt = clock.instant();
    Instant expiresAt = createdAt.plus(refreshSessionDuration);
    String refreshToken = generateToken();
    refreshSessionRepository.save(new RefreshSession(user,
      hashToken(refreshToken),
      createdAt,
      expiresAt));
    return result(user,
      refreshToken,
      expiresAt,
      persistentCookie);
  }

  @Transactional
  public AuthenticationResult refresh(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw unauthorized();
    }
    RefreshSession refreshSession = refreshSessionRepository.findByTokenHash(hashToken(refreshToken))
      .orElseThrow(this::unauthorized);
    Instant now = clock.instant();
    if (refreshSession.getRevokedAt() != null || !refreshSession.getExpiresAt().isAfter(now)) {
      throw unauthorized();
    }
    User user = refreshSession.getUser();
    assertAccountActive(user);
    refreshSessionRepository.updateLastUsedAt(refreshSession.getId(),
      now);
    return result(user,
      refreshToken,
      refreshSession.getExpiresAt(),
      false);
  }

  @Transactional
  public void revoke(String refreshToken) {
    if (refreshToken != null && !refreshToken.isBlank()) {
      refreshSessionRepository.revokeByTokenHash(hashToken(refreshToken),
        clock.instant());
    }
  }

  @Transactional
  public void revokeAll(User user) {
    refreshSessionRepository.revokeAllByUserId(user.getId(),
      clock.instant());
  }

  private AuthenticationResult result(User user,
    String refreshToken,
    Instant refreshExpiresAt,
    boolean persistentCookie) {
    return new AuthenticationResult(jwtService.generateToken(user),
      jwtService.expiresInSeconds(),
      refreshToken,
      refreshExpiresAt,
      persistentCookie);
  }

  private void assertAccountActive(User user) {
    if (!user.isActive()
      || !user.getCompany().isActive()
      || !user.getEmployee().isActive()
      || user.getEmployee().isEnable()
      || user.getEmployee().getStatus() != EmployeeStatus.ACTIVE) {
      throw unauthorized();
    }
  }

  private String generateToken() {
    byte[] tokenBytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(tokenBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
  }

  private String hashToken(String token) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable",
        exception);
    }
  }

  private ResponseStatusException unauthorized() {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
      "Refresh session is invalid");
  }
}
