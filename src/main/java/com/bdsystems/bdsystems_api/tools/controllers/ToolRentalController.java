package com.bdsystems.bdsystems_api.tools.controllers;

import com.bdsystems.bdsystems_api.tools.domains.dtos.ToolRentalRequest;
import com.bdsystems.bdsystems_api.tools.domains.dtos.ToolRentalResponse;
import com.bdsystems.bdsystems_api.tools.services.ToolRentalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/tool-rentals")
public class ToolRentalController {

    private final ToolRentalService toolRentalService;

    public ToolRentalController(ToolRentalService toolRentalService) {
        this.toolRentalService = toolRentalService;
    }

    @PostMapping
    public ResponseEntity<ToolRentalResponse> create(
            @Valid @RequestBody ToolRentalRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toolRentalService.create(request, companyIdFrom(jwt)));
    }

    @GetMapping
    public Page<ToolRentalResponse> findAll(
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return toolRentalService.findAll(companyIdFrom(jwt), pageable);
    }

    @GetMapping("/{id}")
    public ToolRentalResponse findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return toolRentalService.findById(id, companyIdFrom(jwt));
    }

    @PutMapping("/{id}")
    public ToolRentalResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ToolRentalRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return toolRentalService.update(id, request, companyIdFrom(jwt));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        toolRentalService.delete(id, companyIdFrom(jwt));
        return ResponseEntity.noContent().build();
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
