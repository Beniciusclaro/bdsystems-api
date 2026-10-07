package com.bdsystems.bdsystems_api.employee.controllers;

import com.bdsystems.bdsystems_api.employee.domains.dtos.EmployeeRequest;
import com.bdsystems.bdsystems_api.employee.domains.dtos.EmployeeResponse;
import com.bdsystems.bdsystems_api.employee.services.EmployeeService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

  private final EmployeeService employeeService;

  public EmployeeController(EmployeeService employeeService) {
    this.employeeService = employeeService;
  }

  @PostMapping
  public ResponseEntity<EmployeeResponse> create(
      @Valid @RequestBody EmployeeRequest request,
      @AuthenticationPrincipal Jwt jwt
  ) {
    String tenantIdClaim = jwt.getClaimAsString("tenant_id");
    if (tenantIdClaim == null) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "JWT does not contain a valid tenant_id"
      );
    }

    UUID companyId;
    try {
      companyId = UUID.fromString(tenantIdClaim);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "JWT does not contain a valid tenant_id"
      );
    }

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(employeeService.create(request, companyId));
  }
}
