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
  private final CompositionService s;

  public CompositionController(CompositionService s) {
    this.s = s;
  }

  @PostMapping
  ResponseEntity<Response> create(@Valid @RequestBody Request r,
    @AuthenticationPrincipal Jwt j) {
    return ResponseEntity.status(HttpStatus.CREATED).body(s.create(r,
      TenantIds.from(j)));
  }

  @GetMapping
  Page<Response> list(@RequestParam(required = false) Boolean active,
    @RequestParam(required = false) String search,
    Pageable p,
    @AuthenticationPrincipal Jwt j) {
    return s.list(TenantIds.from(j),
      active,
      search,
      p);
  }

  @GetMapping("/{id}")
  Response get(@PathVariable UUID id,
    @AuthenticationPrincipal Jwt j) {
    return s.get(id,
      TenantIds.from(j));
  }

  @PutMapping("/{id}")
  Response update(@PathVariable UUID id,
    @Valid @RequestBody Request r,
    @AuthenticationPrincipal Jwt j) {
    return s.update(id,
      r,
      TenantIds.from(j));
  }

  @PatchMapping("/{id}/status")
  ResponseEntity<Void> status(@PathVariable UUID id,
    @RequestBody Status r,
    @AuthenticationPrincipal Jwt j) {
    s.status(id,
      TenantIds.from(j),
      r.active());
    return ResponseEntity.noContent().build();
  }

  public record Status(boolean active) {
  }
}
