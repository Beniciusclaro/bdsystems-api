package com.bdsystems.bdsystems_api.employee.controllers;

import com.bdsystems.bdsystems_api.employee.domains.dtos.EmployeeRequest;
import com.bdsystems.bdsystems_api.employee.domains.dtos.EmployeeResponse;
import com.bdsystems.bdsystems_api.employee.domains.entities.EmployeeStatus;
import com.bdsystems.bdsystems_api.employee.services.EmployeeService;
import jakarta.validation.Valid;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/employees")
@Slf4j
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
    return ResponseEntity
      .status(HttpStatus.CREATED)
      .body(employeeService.create(request,
        companyIdFrom(jwt)));
  }

  @GetMapping
  public Page<EmployeeResponse> findAll(
    Pageable pageable,
    @RequestParam(required = false) EmployeeStatus status,
    @RequestParam(required = false) String position,
    @RequestParam(required = false) String department,
    @AuthenticationPrincipal Jwt jwt
  ) {
    return employeeService.findAll(companyIdFrom(jwt),
      pageable,
      status,
      position,
      department);
  }

  @PutMapping("/{id}")
  public EmployeeResponse update(
    @PathVariable UUID id,
    @Valid @RequestBody EmployeeRequest request,
    @AuthenticationPrincipal Jwt jwt
  ) {
    return employeeService.update(id,
      request,
      companyIdFrom(jwt));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
    @PathVariable UUID id,
    @AuthenticationPrincipal Jwt jwt
  ) {
    employeeService.delete(id,
      companyIdFrom(jwt));
    log.info("Employee with ID {} deleted successfully for company ID {}",
      id,
      companyIdFrom(jwt));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/user-invitation/resend")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resendUserInvitation(
    @PathVariable UUID id,
    @AuthenticationPrincipal Jwt jwt
  ) {
    employeeService.resendInvitation(id,
      companyIdFrom(jwt));
  }

  private UUID companyIdFrom(Jwt jwt) {
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
    return companyId;
  }
}
