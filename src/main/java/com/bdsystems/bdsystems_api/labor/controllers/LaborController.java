package com.bdsystems.bdsystems_api.labor.controllers;

import com.bdsystems.bdsystems_api.auth.security.TenantIds;
import com.bdsystems.bdsystems_api.labor.domains.dtos.LaborDtos.ProfileRequest;
import com.bdsystems.bdsystems_api.labor.domains.dtos.LaborDtos.ProfileResponse;
import com.bdsystems.bdsystems_api.labor.domains.dtos.LaborDtos.RoleRequest;
import com.bdsystems.bdsystems_api.labor.domains.dtos.LaborDtos.RoleResponse;
import com.bdsystems.bdsystems_api.labor.services.LaborService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/labor-roles")
public class LaborController {
  private final LaborService laborService;

  public LaborController(LaborService laborService) {
    this.laborService = laborService;
  }

  @PostMapping
  ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleRequest request,
    @AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.status(HttpStatus.CREATED).body(laborService.create(request,
      TenantIds.from(jwt)));
  }

  @GetMapping
  Page<RoleResponse> list(@RequestParam(required = false) String search,
    @RequestParam(required = false) Boolean active,
    Pageable pageable,
    @AuthenticationPrincipal Jwt jwt) {
    return laborService.list(TenantIds.from(jwt),
      search,
      active,
      pageable);
  }

  @GetMapping("/reference-positions")
  List<String> referencePositions(@AuthenticationPrincipal Jwt jwt) {
    return laborService.referencePositions(TenantIds.from(jwt));
  }

  @PutMapping("/{id}")
  RoleResponse update(@PathVariable UUID id,
    @Valid @RequestBody RoleRequest request,
    @AuthenticationPrincipal Jwt jwt) {
    return laborService.update(id,
      request,
      TenantIds.from(jwt));
  }

  @PutMapping("/employees/{employeeId}/cost-profile")
  ProfileResponse profile(@PathVariable UUID employeeId,
    @Valid @RequestBody ProfileRequest request,
    @AuthenticationPrincipal Jwt jwt) {
    return laborService.saveProfile(employeeId,
      request,
      TenantIds.from(jwt));
  }

  @GetMapping("/employees/{employeeId}/cost-profile")
  ProfileResponse profile(@PathVariable UUID employeeId,
    @AuthenticationPrincipal Jwt jwt) {
    return laborService.profile(employeeId,
      TenantIds.from(jwt));
  }
}
