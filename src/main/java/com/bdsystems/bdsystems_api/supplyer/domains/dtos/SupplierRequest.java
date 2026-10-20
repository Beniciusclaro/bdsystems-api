package com.bdsystems.bdsystems_api.supplyer.domains.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupplierRequest(
  @NotBlank String legalName,
  String tradeName,
  String taxNumber,
  @Email String email,
  String phoneNumber,
  @NotNull @Valid AddressRequest address
) {
}
