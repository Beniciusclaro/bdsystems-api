package com.bdsystems.bdsystems_api.compositions.controllers;

import com.bdsystems.bdsystems_api.auth.security.TenantIds;
import com.bdsystems.bdsystems_api.compositions.domains.dtos.CompositionDtos.*;
import com.bdsystems.bdsystems_api.compositions.services.CompositionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/compositions")
public class CompositionController {
  private final CompositionService compositionService;

  public CompositionController(CompositionService compositionService) {
    this.compositionService = compositionService;
  }

  @PostMapping
  ResponseEntity<Response> create(@Valid @RequestBody Request request,
    @AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.status(HttpStatus.CREATED).body(compositionService.create(request,
      TenantIds.from(jwt)));
  }

  @GetMapping
  Page<Response> list(@RequestParam(required = false) Boolean active,
    @RequestParam(required = false) String search,
    Pageable pageable,
    @AuthenticationPrincipal Jwt jwt) {
    return compositionService.list(TenantIds.from(jwt),
      active,
      search,
      pageable);
  }

  @GetMapping("/{id}")
  Response get(@PathVariable UUID id,
    @AuthenticationPrincipal Jwt jwt) {
    return compositionService.get(id,
      TenantIds.from(jwt));
  }

  @PutMapping("/{id}")
  Response update(@PathVariable UUID id,
    @Valid @RequestBody Request request,
    @AuthenticationPrincipal Jwt jwt) {
    return compositionService.update(id,
      request,
      TenantIds.from(jwt));
  }

  @PutMapping("/{compositionId}/components/{componentId}")
  Response updateComponent(@PathVariable UUID compositionId,
    @PathVariable UUID componentId,
    @Valid @RequestBody ComponentRequest request,
    @AuthenticationPrincipal Jwt jwt) {
    return compositionService.updateComponent(compositionId,
      componentId,
      request,
      TenantIds.from(jwt));
  }

  @DeleteMapping("/{compositionId}/components/{componentId}")
  ResponseEntity<Void> deleteComponent(@PathVariable UUID compositionId,
    @PathVariable UUID componentId,
    @AuthenticationPrincipal Jwt jwt) {
    compositionService.deleteComponent(compositionId,
      componentId,
      TenantIds.from(jwt));
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/status")
  ResponseEntity<Void> status(@PathVariable UUID id,
    @RequestBody Status request,
    @AuthenticationPrincipal Jwt jwt) {
    compositionService.status(id,
      TenantIds.from(jwt),
      request.active());
    return ResponseEntity.noContent().build();
  }

  public record Status(boolean active) {
  }
}
