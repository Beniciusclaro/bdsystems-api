package com.bdsystems.bdsystems_api.auth.domains.dtos;

import jakarta.validation.constraints.NotBlank;

public record InvitationAcceptanceRequest(
  @NotBlank String token,
  @NotBlank String password
) {
}
