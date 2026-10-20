package com.bdsystems.bdsystems_api.materials.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

final class MaterialTenant {
  private MaterialTenant() {
  }

  static UUID companyId(Jwt jwt) {
    try {
      if (jwt == null || jwt.getClaimAsString("tenant_id") == null) throw new IllegalArgumentException();
      return UUID.fromString(jwt.getClaimAsString("tenant_id"));
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
        "JWT does not contain a valid tenant_id");
    }
  }
}
