package com.bdsystems.bdsystems_api.tools.services;

import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.supplyer.domains.entities.Supplier;
import com.bdsystems.bdsystems_api.supplyer.repositories.SupplierRepository;
import com.bdsystems.bdsystems_api.tools.domains.dtos.ToolRentalRequest;
import com.bdsystems.bdsystems_api.tools.domains.dtos.ToolRentalResponse;
import com.bdsystems.bdsystems_api.tools.domains.entities.Tool;
import com.bdsystems.bdsystems_api.tools.domains.entities.ToolRental;
import com.bdsystems.bdsystems_api.tools.repositories.ToolRentalRepository;
import com.bdsystems.bdsystems_api.tools.repositories.ToolRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class ToolRentalService {

    private final ToolRentalRepository toolRentalRepository;
    private final ToolRepository toolRepository;
    private final SupplierRepository supplierRepository;
    private final CompanyRepository companyRepository;

    public ToolRentalService(
            ToolRentalRepository toolRentalRepository,
            ToolRepository toolRepository,
            SupplierRepository supplierRepository,
            CompanyRepository companyRepository
    ) {
        this.toolRentalRepository = toolRentalRepository;
        this.toolRepository = toolRepository;
        this.supplierRepository = supplierRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public ToolRentalResponse create(ToolRentalRequest request, UUID companyId) {
        validate(request);
        Company company = findCompany(companyId);
        Tool tool = findActiveTool(request.toolId(), companyId);
        Supplier supplier = findActiveSupplier(request.supplierId(), companyId);
        return toResponse(toolRentalRepository.save(new ToolRental(
                company,
                tool,
                supplier,
                request.price(),
                request.rentalPeriod(),
                request.rentalStartDate(),
                request.rentalEndDate()
        )));
    }

    @Transactional(readOnly = true)
    public Page<ToolRentalResponse> findAll(UUID companyId, Pageable pageable) {
        return toolRentalRepository.findByCompanyId(companyId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ToolRentalResponse findById(UUID id, UUID companyId) {
        return toResponse(findRental(id, companyId));
    }

    @Transactional(readOnly = true)
    public Page<ToolRentalResponse> findByToolId(
            UUID toolId,
            UUID companyId,
            Pageable pageable
    ) {
        toolRepository.findByIdAndCompanyId(toolId, companyId)
                .orElseThrow(() -> notFound("Tool not found"));
        return toolRentalRepository.findByCompanyIdAndToolId(companyId, toolId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ToolRentalResponse update(UUID id, ToolRentalRequest request, UUID companyId) {
        validate(request);
        ToolRental rental = findRental(id, companyId);
        Tool tool = findActiveTool(request.toolId(), companyId);
        Supplier supplier = findActiveSupplier(request.supplierId(), companyId);
        rental.updateDetails(
                tool,
                supplier,
                request.price(),
                request.rentalPeriod(),
                request.rentalStartDate(),
                request.rentalEndDate()
        );
        return toResponse(toolRentalRepository.save(rental));
    }

    @Transactional
    public void delete(UUID id, UUID companyId) {
        toolRentalRepository.delete(findRental(id, companyId));
    }

    private Company findCompany(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Authenticated company not found"
                ));
    }

    private Tool findActiveTool(UUID toolId, UUID companyId) {
        return toolRepository.findByIdAndCompanyIdAndActiveTrue(toolId, companyId)
                .orElseThrow(() -> notFound("Active tool not found"));
    }

    private Supplier findActiveSupplier(UUID supplierId, UUID companyId) {
        return supplierRepository.findByIdAndCompanyIdAndActiveTrue(supplierId, companyId)
                .orElseThrow(() -> notFound("Active supplier not found"));
    }

    private ToolRental findRental(UUID id, UUID companyId) {
        return toolRentalRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> notFound("Tool rental not found"));
    }

    private void validate(ToolRentalRequest request) {
        if (request == null || request.toolId() == null || request.supplierId() == null
                || request.price() == null || request.rentalPeriod() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tool, supplier, price and rental period are required"
            );
        }
        if (request.price().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price cannot be negative");
        }
        LocalDate startDate = request.rentalStartDate();
        LocalDate endDate = request.rentalEndDate();
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rental end date cannot be before rental start date"
            );
        }
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private ToolRentalResponse toResponse(ToolRental rental) {
        return new ToolRentalResponse(
                rental.getId(),
                rental.getCompany().getId(),
                rental.getTool().getId(),
                rental.getSupplier().getId(),
                rental.getPrice(),
                rental.getRentalPeriod(),
                rental.getRentalStartDate(),
                rental.getRentalEndDate(),
                rental.getCreatedAt(),
                rental.getUpdatedAt()
        );
    }
}
