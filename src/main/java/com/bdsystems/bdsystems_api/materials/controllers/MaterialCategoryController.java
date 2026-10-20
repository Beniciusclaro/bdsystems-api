package com.bdsystems.bdsystems_api.materials.controllers;

import com.bdsystems.bdsystems_api.materials.domains.dtos.MaterialCategoryRequest;
import com.bdsystems.bdsystems_api.materials.domains.dtos.MaterialCategoryResponse;
import com.bdsystems.bdsystems_api.materials.domains.dtos.StatusRequest;
import com.bdsystems.bdsystems_api.materials.services.MaterialService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/material-categories")
public class MaterialCategoryController {

  private final MaterialService service;

  public MaterialCategoryController(MaterialService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<MaterialCategoryResponse> create(
    @Valid @RequestBody MaterialCategoryRequest request,
    @AuthenticationPrincipal Jwt jwt
  ) {
    return ResponseEntity.status(HttpStatus.CREATED)
      .body(service.createCategory(request,
        MaterialTenant.companyId(jwt)));
  }

  @GetMapping
  public Page<MaterialCategoryResponse> findAll(
    Pageable pageable,
    @RequestParam(required = false) Boolean active,
    @AuthenticationPrincipal Jwt jwt
  ) {
    return service.findCategories(MaterialTenant.companyId(jwt),
      active,
      pageable);
  }

  @PutMapping("/{id}")
  public MaterialCategoryResponse update(
    @PathVariable UUID id,
    @Valid @RequestBody MaterialCategoryRequest request,
    @AuthenticationPrincipal Jwt jwt
  ) {

    return service.updateCategory(id,
      request,
      MaterialTenant.companyId(jwt));
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<Void> status(
    @PathVariable UUID id,
    @Valid @RequestBody StatusRequest request,
    @AuthenticationPrincipal Jwt jwt
  ) {

    service.changeCategoryStatus(id,
      MaterialTenant.companyId(jwt),
      request.active());
    return ResponseEntity.noContent().build();
  }
}
