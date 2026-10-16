package com.bdsystems.bdsystems_api.supplyer.services;

import com.bdsystems.bdsystems_api.auth.domains.entities.Address;
import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.repositories.AddressRepository;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.supplyer.domains.dtos.SupplierRequest;
import com.bdsystems.bdsystems_api.supplyer.domains.dtos.SupplierResponse;
import com.bdsystems.bdsystems_api.supplyer.domains.entities.Supplier;
import com.bdsystems.bdsystems_api.supplyer.repositories.SupplierRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final CompanyRepository companyRepository;
    private final AddressRepository addressRepository;

    public SupplierService(
            SupplierRepository supplierRepository,
            CompanyRepository companyRepository,
            AddressRepository addressRepository
    ) {
        this.supplierRepository = supplierRepository;
        this.companyRepository = companyRepository;
        this.addressRepository = addressRepository;
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request, UUID companyId) {
        if (request.taxNumber() != null
                && supplierRepository.existsByCompanyIdAndTaxNumber(companyId, request.taxNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Supplier tax number already registered for this company"
            );
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Company not found"
                ));
        Address address = addressRepository.save(new Address(
                request.address().street(),
                request.address().city(),
                request.address().state(),
                request.address().zipCode()
        ));
        Supplier supplier = new Supplier(
                company,
                request.legalName(),
                request.tradeName(),
                request.taxNumber(),
                request.email(),
                request.phoneNumber(),
                address
        );

        return toResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public Page<SupplierResponse> findAll(UUID companyId, Pageable pageable) {
        return supplierRepository.findByCompanyIdAndActiveTrue(companyId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public SupplierResponse findById(UUID id, UUID companyId) {
        Supplier supplier = findActiveSupplier(id, companyId);
        return toResponse(supplier);
    }

    @Transactional
    public SupplierResponse update(UUID id, SupplierRequest request, UUID companyId) {
        Supplier supplier = findActiveSupplier(id, companyId);

        if (request.taxNumber() != null
                && supplierRepository.existsByCompanyIdAndTaxNumberAndIdNot(
                        companyId,
                        request.taxNumber(),
                        id
                )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Supplier tax number already registered for this company"
            );
        }

        supplier.updateDetails(
                request.legalName(),
                request.tradeName(),
                request.taxNumber(),
                request.email(),
                request.phoneNumber()
        );
        supplier.getAddress().update(
                request.address().street(),
                request.address().city(),
                request.address().state(),
                request.address().zipCode()
        );

        return toResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public void delete(UUID id, UUID companyId) {
        Supplier supplier = findActiveSupplier(id, companyId);
        supplier.markAsDeleted();
        supplierRepository.save(supplier);
    }

    private Supplier findActiveSupplier(UUID id, UUID companyId) {
        return supplierRepository.findByIdAndCompanyIdAndActiveTrue(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Supplier not found"
                ));
    }

    private SupplierResponse toResponse(Supplier supplier) {
        Address address = supplier.getAddress();
        return new SupplierResponse(
                supplier.getId(),
                supplier.getLegalName(),
                supplier.getTradeName(),
                supplier.getTaxNumber(),
                supplier.getEmail(),
                supplier.getPhoneNumber(),
                new SupplierResponse.AddressResponse(
                        address.getStreet(),
                        address.getCity(),
                        address.getState(),
                        address.getZipCode()
                ),
                supplier.getCompany().getId(),
                supplier.isActive()
        );
    }
}
