package com.bdsystems.bdsystems_api.auth.services;

import java.time.Instant;

public record AuthenticationResult(
  String accessToken,
  long expiresInSeconds,
  String refreshToken,
  Instant refreshExpiresAt,
  boolean persistentCookie
) {
}
