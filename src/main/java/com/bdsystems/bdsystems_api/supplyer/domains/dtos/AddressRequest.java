package com.bdsystems.bdsystems_api.supplyer.domains.dtos;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
  @NotBlank String street,
  @NotBlank String city,
  @NotBlank String state,
  @NotBlank String zipCode
) {
}
