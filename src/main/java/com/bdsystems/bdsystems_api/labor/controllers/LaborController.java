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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/labor-roles")
public class LaborController {
  private final LaborService s;

  public LaborController(LaborService s) {
    this.s = s;
  }

  @PostMapping
  ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleRequest r,
    @AuthenticationPrincipal Jwt j) {
    return ResponseEntity.status(HttpStatus.CREATED).body(s.create(r,
      TenantIds.from(j)));
  }

  @GetMapping
  Page<RoleResponse> list(Pageable p,
    @AuthenticationPrincipal Jwt j) {
    return s.list(TenantIds.from(j),
      p);
  }

  @PutMapping("/{id}")
  RoleResponse update(@PathVariable UUID id,
    @Valid @RequestBody RoleRequest r,
    @AuthenticationPrincipal Jwt j) {
    return s.update(id,
      r,
      TenantIds.from(j));
  }

  @PutMapping("/employees/{employeeId}/cost-profile")
  ProfileResponse profile(@PathVariable UUID employeeId,
    @Valid @RequestBody ProfileRequest r,
    @AuthenticationPrincipal Jwt j) {
    return s.saveProfile(employeeId,
      r,
      TenantIds.from(j));
  }

  @GetMapping("/employees/{employeeId}/cost-profile")
  ProfileResponse profile(@PathVariable UUID employeeId,
    @AuthenticationPrincipal Jwt j) {
    return s.profile(employeeId,
      TenantIds.from(j));
  }
}
