package com.bdsystems.bdsystems_api.projects.controllers;

import com.bdsystems.bdsystems_api.auth.security.TenantIds;
import com.bdsystems.bdsystems_api.projects.domains.dtos.ProjectDtos.Request;
import com.bdsystems.bdsystems_api.projects.domains.dtos.ProjectDtos.Response;
import com.bdsystems.bdsystems_api.projects.services.ProjectService;
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

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
  private final ProjectService s;

  public ProjectController(ProjectService s) {
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
    @RequestParam(required = false) UUID clientId,
    @RequestParam(required = false) String search,
    Pageable p,
    @AuthenticationPrincipal Jwt j) {
    return s.list(TenantIds.from(j),
      active,
      clientId,
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
}
