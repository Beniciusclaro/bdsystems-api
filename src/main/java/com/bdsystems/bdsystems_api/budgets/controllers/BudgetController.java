package com.bdsystems.bdsystems_api.budgets.controllers;

import com.bdsystems.bdsystems_api.auth.security.TenantIds;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.ItemRequest;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.ApprovalRequest;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.ApprovalResponse;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.Proposal;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.QuantityRequest;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.Request;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.Response;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.StageRequest;
import com.bdsystems.bdsystems_api.budgets.services.BudgetService;
import com.bdsystems.bdsystems_api.budgets.domains.entities.BudgetStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/budgets")
public class BudgetController {
  private final BudgetService s;

  public BudgetController(BudgetService s) {
    this.s = s;
  }

  @PostMapping
  ResponseEntity<Response> create(@Valid @RequestBody Request r,
    @AuthenticationPrincipal Jwt j) {
    return ResponseEntity.status(HttpStatus.CREATED).body(s.create(r,
      TenantIds.from(j)));
  }

  @GetMapping
  Page<Response> list(@RequestParam(required = false) String search,
    @RequestParam(required = false) BudgetStatus status,
    Pageable pageable,
    @AuthenticationPrincipal Jwt jwt) {
    return s.list(TenantIds.from(jwt),
      search,
      status,
      pageable);
  }

  @GetMapping("/{id}")
  Response get(@PathVariable UUID id,
    @AuthenticationPrincipal Jwt j) {
    return s.get(id,
      TenantIds.from(j));
  }

  @GetMapping("/{id}/proposal")
  Proposal proposal(@PathVariable UUID id,
    @AuthenticationPrincipal Jwt j) {
    return s.proposal(id,
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

  @PostMapping("/{id}/stages")
  Response addStage(@PathVariable UUID id,
    @Valid @RequestBody StageRequest r,
    @AuthenticationPrincipal Jwt j) {
    return s.addStage(id,
      r,
      TenantIds.from(j));
  }

  @PutMapping("/{id}/stages/{stageId}")
  Response updateStage(@PathVariable UUID id,
    @PathVariable UUID stageId,
    @Valid @RequestBody StageRequest r,
    @AuthenticationPrincipal Jwt j) {
    return s.updateStage(id,
      stageId,
      r,
      TenantIds.from(j));
  }

  @DeleteMapping("/{id}/stages/{stageId}")
  Response deleteStage(@PathVariable UUID id,
    @PathVariable UUID stageId,
    @AuthenticationPrincipal Jwt j) {
    return s.deleteStage(id,
      stageId,
      TenantIds.from(j));
  }

  @PostMapping("/{id}/stages/{stageId}/items")
  Response addItem(@PathVariable UUID id,
    @PathVariable UUID stageId,
    @Valid @RequestBody ItemRequest r,
    @AuthenticationPrincipal Jwt j) {
    return s.addItem(id,
      stageId,
      r,
      TenantIds.from(j));
  }

  @PatchMapping("/{id}/items/{itemId}/quantity")
  Response quantity(@PathVariable UUID id,
    @PathVariable UUID itemId,
    @Valid @RequestBody QuantityRequest r,
    @AuthenticationPrincipal Jwt j) {
    return s.quantity(id,
      itemId,
      r,
      TenantIds.from(j));
  }

  @PostMapping("/{id}/items/{itemId}/refresh")
  Response refresh(@PathVariable UUID id,
    @PathVariable UUID itemId,
    @AuthenticationPrincipal Jwt j) {
    return s.refresh(id,
      itemId,
      TenantIds.from(j));
  }

  @DeleteMapping("/{id}/items/{itemId}")
  Response deleteItem(@PathVariable UUID id,
    @PathVariable UUID itemId,
    @AuthenticationPrincipal Jwt j) {
    return s.deleteItem(id,
      itemId,
      TenantIds.from(j));
  }

  @PostMapping("/{id}/finalize")
  Response finalizeBudget(@PathVariable UUID id,
    @AuthenticationPrincipal Jwt j) {
    return s.finalizeBudget(id,
      TenantIds.from(j));
  }

  @PostMapping("/{id}/approve-and-create-project")
  ApprovalResponse approveAndCreateProject(@PathVariable UUID id,
    @Valid @RequestBody ApprovalRequest request,
    @AuthenticationPrincipal Jwt jwt) {
    return s.approveAndCreateProject(id,
      request,
      TenantIds.from(jwt));
  }

  @PostMapping("/{id}/reject")
  Response reject(@PathVariable UUID id,
    @AuthenticationPrincipal Jwt jwt) {
    return s.reject(id,
      TenantIds.from(jwt));
  }

  @PostMapping("/{id}/duplicate")
  Response duplicate(@PathVariable UUID id,
    @RequestBody DuplicateRequest r,
    @AuthenticationPrincipal Jwt j) {
    return s.duplicate(id,
      r.number(),
      TenantIds.from(j));
  }

  public record DuplicateRequest(String number) {
  }
}
