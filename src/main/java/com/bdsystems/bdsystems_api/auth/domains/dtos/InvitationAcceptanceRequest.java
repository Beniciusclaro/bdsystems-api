package com.bdsystems.bdsystems_api.auth.domains.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvitationAcceptanceRequest(
  @NotBlank String token,
  @NotBlank @Size(min = 8) String password
) {
}
