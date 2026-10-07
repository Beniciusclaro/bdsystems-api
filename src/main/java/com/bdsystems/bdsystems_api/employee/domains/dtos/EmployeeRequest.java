package com.bdsystems.bdsystems_api.employee.domains.dtos;

import com.bdsystems.bdsystems_api.employee.domains.entities.EmployeeStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EmployeeRequest(
    @NotBlank String fullName,
    @NotBlank @Email String email,
    @NotBlank String phoneNumber,
    String documentNumber,
    LocalDate birthDate,
    @NotNull @Valid AddressRequest address,
    String employeeNumber,
    @NotBlank String position,
    String department,
    LocalDate admissionDate,
    LocalDate terminationDate,
    EmployeeStatus status
) {

  public record AddressRequest(
      @NotBlank String street,
      @NotBlank String city,
      @NotBlank String state,
      @NotBlank String zipCode
  ) {
  }
}
