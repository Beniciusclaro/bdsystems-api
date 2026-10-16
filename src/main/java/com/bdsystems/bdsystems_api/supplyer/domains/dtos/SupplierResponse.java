package com.bdsystems.bdsystems_api.supplyer.domains.dtos;

import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String legalName,
        String tradeName,
        String taxNumber,
        String email,
        String phoneNumber,
        AddressResponse address,
        UUID companyId,
        boolean active
) {
    public record AddressResponse(
            String street,
            String city,
            String state,
            String zipCode
    ) {
    }
}
