package com.bdsystems.bdsystems_api.supplyer.controllers;

import com.bdsystems.bdsystems_api.supplyer.domains.dtos.SupplierRequest;
import com.bdsystems.bdsystems_api.supplyer.domains.dtos.SupplierResponse;
import com.bdsystems.bdsystems_api.supplyer.services.SupplierService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
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

@RestController
@RequestMapping("/api/suppliers")
@Slf4j
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(
            @Valid @RequestBody SupplierRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supplierService.create(request, companyIdFrom(jwt)));
    }

    @GetMapping
    public Page<SupplierResponse> findAll(
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return supplierService.findAll(companyIdFrom(jwt), pageable);
    }

    @GetMapping("/{id}")
    public SupplierResponse findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return supplierService.findById(id, companyIdFrom(jwt));
    }

    @PutMapping("/{id}")
    public SupplierResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return supplierService.update(id, request, companyIdFrom(jwt));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID companyId = companyIdFrom(jwt);
        supplierService.delete(id, companyId);
        log.info("Supplier with ID {} deleted successfully for company ID {}", id, companyId);
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
