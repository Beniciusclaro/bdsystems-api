package com.bdsystems.bdsystems_api.auth.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public final class TenantIds {
  private TenantIds() {
  }

  public static UUID from(Jwt jwt) {
    try {
      String value = jwt == null ? null : jwt.getClaimAsString("tenant_id");
      if (value == null) throw new IllegalArgumentException();
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
        "JWT does not contain a valid tenant_id");
    }
  }
}
