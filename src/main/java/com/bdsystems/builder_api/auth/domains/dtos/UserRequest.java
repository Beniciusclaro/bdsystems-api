package com.bdsystems.builder_api.auth.domains.dtos;

import java.util.UUID;

public record UserRequest(
    String name,
    String email,
    String password,
    String role,
    String companyName,
    AddressRequest address,
    CompanyRequest company) {

  public record AddressRequest(
      String street,
      String city,
      String state,
      String zipcode
  ){
  }

  public record CompanyRequest(
      String name,
      UUID id,
      AddressRequest address
  ){
  }
}
