package com.bdsystems.bdsystems_api.employee.domains.dtos;

import com.bdsystems.bdsystems_api.employee.domains.entities.EmployeeStatus;
import com.bdsystems.bdsystems_api.employee.domains.entities.PaymentMethod;
import com.bdsystems.bdsystems_api.auth.domains.entities.Role;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(
  UUID id,
  String fullName,
  String email,
  String phoneNumber,
  String documentNumber,
  LocalDate birthDate,
  AddressResponse address,
  UUID companyId,
  String employeeNumber,
  String position,
  String department,
  LocalDate admissionDate,
  LocalDate terminationDate,
  EmployeeStatus status,
  PaymentMethod paymentMethod,
  BigDecimal paymentAmount,
  Role userRole,
  UserAccessStatus userAccessStatus
) {


  public record AddressResponse(
    String street,
    String city,
    String state,
    String zipCode
  ) {
  }
}
