package com.bdsystems.bdsystems_api.materials.controllers;

import com.bdsystems.bdsystems_api.materials.domains.dtos.UnitOfMeasureResponse;
import com.bdsystems.bdsystems_api.materials.services.MaterialService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/units-of-measure")
public class UnitOfMeasureController {
  private final MaterialService service;

  public UnitOfMeasureController(MaterialService service) {
    this.service = service;
  }

  @GetMapping
  public Page<UnitOfMeasureResponse> findAll(Pageable pageable,
    @RequestParam(defaultValue = "true") boolean active,
    @AuthenticationPrincipal Jwt jwt) {
    MaterialTenant.companyId(jwt);
    return service.findUnits(active,
      pageable);
  }
}
