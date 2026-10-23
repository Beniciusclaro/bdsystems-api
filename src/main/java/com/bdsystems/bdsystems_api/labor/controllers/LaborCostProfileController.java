package com.bdsystems.bdsystems_api.labor.controllers;

import com.bdsystems.bdsystems_api.auth.security.TenantIds;
import com.bdsystems.bdsystems_api.labor.domains.dtos.LaborDtos.ProfileListResponse;
import com.bdsystems.bdsystems_api.labor.services.LaborService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/labor-cost-profiles")
public class LaborCostProfileController {
  private final LaborService laborService;

  public LaborCostProfileController(LaborService laborService) {
    this.laborService = laborService;
  }

  @GetMapping
  Page<ProfileListResponse> list(@RequestParam(required = false) String search,
    @RequestParam(required = false) Boolean configured,
    Pageable pageable,
    @AuthenticationPrincipal Jwt jwt) {
    return laborService.listProfiles(TenantIds.from(jwt),
      search,
      configured,
      pageable);
  }
}
