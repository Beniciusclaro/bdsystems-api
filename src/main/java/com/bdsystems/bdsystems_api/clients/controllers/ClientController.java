package com.bdsystems.bdsystems_api.clients.controllers;

import com.bdsystems.bdsystems_api.auth.security.TenantIds;
import com.bdsystems.bdsystems_api.clients.domains.dtos.ClientDtos.Request;
import com.bdsystems.bdsystems_api.clients.domains.dtos.ClientDtos.Response;
import com.bdsystems.bdsystems_api.clients.services.ClientService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
public class ClientController {
  private final ClientService service;

  public ClientController(ClientService service) {
    this.service = service;
  }

  @PostMapping
  ResponseEntity<Response> create(@Valid @RequestBody Request r,
    @AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r,
      TenantIds.from(jwt)));
  }

  @GetMapping
  Page<Response> list(@RequestParam(required = false) Boolean active,
    @RequestParam(required = false) String search,
    Pageable p,
    @AuthenticationPrincipal Jwt jwt) {
    return service.list(TenantIds.from(jwt),
      active,
      search,
      p);
  }

  @GetMapping("/{id}")
  Response get(@PathVariable UUID id,
    @AuthenticationPrincipal Jwt jwt) {
    return service.get(id,
      TenantIds.from(jwt));
  }

  @PutMapping("/{id}")
  Response update(@PathVariable UUID id,
    @Valid @RequestBody Request r,
    @AuthenticationPrincipal Jwt jwt) {
    return service.update(id,
      r,
      TenantIds.from(jwt));
  }

  @PatchMapping("/{id}/status")
  ResponseEntity<Void> status(@PathVariable UUID id,
    @RequestBody Status r,
    @AuthenticationPrincipal Jwt jwt) {
    service.status(id,
      TenantIds.from(jwt),
      r.active());
    return ResponseEntity.noContent().build();
  }

  public record Status(boolean active) {
  }
}
