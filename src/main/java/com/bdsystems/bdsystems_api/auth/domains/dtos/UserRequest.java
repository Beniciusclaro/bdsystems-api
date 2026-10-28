package com.bdsystems.bdsystems_api.auth.domains.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
  @NotBlank String name,
  @NotBlank
  @Email(message = "E-mail inválido")
  String email,
  @NotBlank @Size(min = 8) String password,
  @NotNull @Valid CompanyRequest company
) {

  public record AddressRequest(
    @NotBlank String street,
    @NotBlank String city,
    @NotBlank String state,
    @NotBlank String zipcode
  ) {
  }

  public record CompanyRequest(
    @NotBlank String name,
    @NotNull @Valid AddressRequest address
  ) {
  }
}
