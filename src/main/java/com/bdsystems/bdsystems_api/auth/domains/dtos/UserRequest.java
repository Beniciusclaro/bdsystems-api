package com.bdsystems.bdsystems_api.auth.domains.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record UserRequest(
    @NotBlank String name,
    @NotBlank
    @Email(message = "E-mail inválido")
    String email,
    @NotBlank String password,
    @Valid CompanyRequest company) {

  public record AddressRequest(
      String street,
      String city,
      String state,
      String zipcode
  ){
  }

  public record CompanyRequest(
      @NotBlank String name,
      UUID id,
      AddressRequest address
  ){
  }
}
