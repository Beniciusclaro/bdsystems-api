package com.bdsystems.bdsystems_api.materials.controllers;

import com.bdsystems.bdsystems_api.materials.domains.dtos.*;
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
@RequestMapping("/api/materials")
public class MaterialController {
  private final MaterialService service;

  public MaterialController(MaterialService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<MaterialResponse> create(
    @Valid @RequestBody MaterialRequest request,
    @AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request,
      MaterialTenant.companyId(jwt)));
  }

  @GetMapping
  public Page<MaterialResponse> findAll(
    Pageable pageable,
    @RequestParam(required = false) Boolean active,
    @RequestParam(required = false) UUID categoryId,
    @RequestParam(required = false) String name,
    @RequestParam(required = false) String code,
    @AuthenticationPrincipal Jwt jwt
  ) {
    return service.findAll(MaterialTenant.companyId(jwt),
      active,
      categoryId,
      name,
      code,
      pageable);
  }

  @GetMapping("/{id}")
  public MaterialResponse findById(@PathVariable UUID id,
    @AuthenticationPrincipal Jwt jwt) {
    return service.findById(id,
      MaterialTenant.companyId(jwt));
  }

  @PutMapping("/{id}")
  public MaterialResponse update(@PathVariable UUID id,
    @Valid @RequestBody MaterialRequest request,
    @AuthenticationPrincipal Jwt jwt) {
    return service.update(id,
      request,
      MaterialTenant.companyId(jwt));
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<Void> status(@PathVariable UUID id,
    @Valid @RequestBody StatusRequest request,
    @AuthenticationPrincipal Jwt jwt) {
    service.changeStatus(id,
      MaterialTenant.companyId(jwt),
      request.active());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/suppliers")
  public Page<SupplierMaterialResponse> suppliers(@PathVariable UUID id,
    Pageable pageable,
    @AuthenticationPrincipal Jwt jwt) {
    return service.findSuppliers(id,
      MaterialTenant.companyId(jwt),
      pageable);
  }

  @PostMapping("/{id}/suppliers")
  public ResponseEntity<SupplierMaterialResponse> addSupplier(@PathVariable UUID id,
    @Valid @RequestBody SupplierMaterialRequest request,
    @AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.addSupplier(id,
      request,
      MaterialTenant.companyId(jwt)));
  }

  @PutMapping("/{id}/suppliers/{associationId}")
  public SupplierMaterialResponse updateSupplier(@PathVariable UUID id,
    @PathVariable UUID associationId,
    @Valid @RequestBody SupplierMaterialRequest request,
    @AuthenticationPrincipal Jwt jwt) {
    return service.updateSupplier(id,
      associationId,
      request,
      MaterialTenant.companyId(jwt));
  }

  @DeleteMapping("/{id}/suppliers/{associationId}")
  public ResponseEntity<Void> removeSupplier(@PathVariable UUID id,
    @PathVariable UUID associationId,
    @AuthenticationPrincipal Jwt jwt) {
    service.removeSupplier(id,
      associationId,
      MaterialTenant.companyId(jwt));
    return ResponseEntity.noContent().build();
  }
}
