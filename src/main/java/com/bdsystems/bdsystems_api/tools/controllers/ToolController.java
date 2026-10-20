package com.bdsystems.bdsystems_api.tools.controllers;

import com.bdsystems.bdsystems_api.tools.domains.dtos.ToolRequest;
import com.bdsystems.bdsystems_api.tools.domains.dtos.ToolResponse;
import com.bdsystems.bdsystems_api.tools.domains.dtos.ToolRentalResponse;
import com.bdsystems.bdsystems_api.tools.services.ToolRentalService;
import com.bdsystems.bdsystems_api.tools.services.ToolService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

  private final ToolService toolService;
  private final ToolRentalService toolRentalService;

  public ToolController(ToolService toolService,
    ToolRentalService toolRentalService) {
    this.toolService = toolService;
    this.toolRentalService = toolRentalService;
  }

  @PostMapping
  public ResponseEntity<ToolResponse> create(
    @Valid @RequestBody ToolRequest request,
    @AuthenticationPrincipal Jwt jwt
  ) {
    return ResponseEntity.status(HttpStatus.CREATED)
      .body(toolService.create(request,
        companyIdFrom(jwt)));
  }

  @GetMapping
  public Page<ToolResponse> findAll(
    Pageable pageable,
    @RequestParam(required = false) Boolean active,
    @RequestParam(required = false) String q,
    @AuthenticationPrincipal Jwt jwt
  ) {
    return toolService.findAll(companyIdFrom(jwt),
      pageable,
      active,
      q);
  }

  @GetMapping("/{id}")
  public ToolResponse findById(
    @PathVariable UUID id,
    @AuthenticationPrincipal Jwt jwt
  ) {
    return toolService.findById(id,
      companyIdFrom(jwt));
  }

  @PutMapping("/{id}")
  public ToolResponse update(
    @PathVariable UUID id,
    @Valid @RequestBody ToolRequest request,
    @AuthenticationPrincipal Jwt jwt
  ) {
    return toolService.update(id,
      request,
      companyIdFrom(jwt));
  }

  @PatchMapping("/{id}/activate")
  public ResponseEntity<Void> activate(
    @PathVariable UUID id,
    @AuthenticationPrincipal Jwt jwt
  ) {
    toolService.activate(id,
      companyIdFrom(jwt));
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/deactivate")
  public ResponseEntity<Void> deactivate(
    @PathVariable UUID id,
    @AuthenticationPrincipal Jwt jwt
  ) {
    toolService.deactivate(id,
      companyIdFrom(jwt));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{toolId}/rentals")
  public Page<ToolRentalResponse> findRentals(
    @PathVariable UUID toolId,
    Pageable pageable,
    @AuthenticationPrincipal Jwt jwt
  ) {
    return toolRentalService.findByToolId(toolId,
      companyIdFrom(jwt),
      pageable);
  }

  private UUID companyIdFrom(Jwt jwt) {
    String tenantIdClaim = jwt.getClaimAsString("tenant_id");
    if (tenantIdClaim == null) {
      throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "JWT does not contain a valid tenant_id"
      );
    }
    try {
      return UUID.fromString(tenantIdClaim);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "JWT does not contain a valid tenant_id"
      );
    }
  }
}
