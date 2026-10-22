package com.bdsystems.bdsystems_api.compositions.services;

import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.compositions.domains.dtos.CompositionDtos.*;
import com.bdsystems.bdsystems_api.compositions.domains.entities.*;
import com.bdsystems.bdsystems_api.compositions.repositories.CompositionRepository;
import com.bdsystems.bdsystems_api.labor.domains.entities.LaborRole;
import com.bdsystems.bdsystems_api.labor.services.LaborService;
import com.bdsystems.bdsystems_api.materials.domains.entities.*;
import com.bdsystems.bdsystems_api.materials.repositories.*;
import com.bdsystems.bdsystems_api.tools.domains.entities.ToolRental;
import com.bdsystems.bdsystems_api.tools.repositories.ToolRentalRepository;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.*;
import java.util.*;

@Service
public class CompositionService {
  private final CompositionRepository compositions;
  private final CompanyRepository companies;
  private final UnitOfMeasureRepository units;
  private final MaterialRepository materials;
  private final SupplierMaterialRepository suppliers;
  private final ToolRentalRepository rentals;
  private final LaborService labor;

  public CompositionService(CompositionRepository compositions,
    CompanyRepository companies,
    UnitOfMeasureRepository units,
    MaterialRepository materials,
    SupplierMaterialRepository suppliers,
    ToolRentalRepository rentals,
    LaborService labor) {
    this.compositions = compositions;
    this.companies = companies;
    this.units = units;
    this.materials = materials;
    this.suppliers = suppliers;
    this.rentals = rentals;
    this.labor = labor;
  }

  @Transactional
  public Response create(Request request,
    UUID tenantId) {
    return save(new Composition(companies.findById(tenantId).orElseThrow(() -> error(HttpStatus.FORBIDDEN,
        "Authenticated company not found"))),
      request,
      tenantId);
  }

  @Transactional
  public Response update(UUID id,
    Request request,
    UUID tenantId) {
    return save(find(id,
        tenantId),
      request,
      tenantId);
  }

  @Transactional
  public Response updateComponent(UUID compositionId,
    UUID componentId,
    ComponentRequest request,
    UUID tenantId) {
    Composition composition = find(compositionId,
      tenantId);
    CompositionComponent compositionComponent = findComponent(composition,
      componentId);
    CompositionComponent updatedValues = build(request,
      tenantId,
      compositionComponent.getSortOrder());
    compositionComponent.update(updatedValues.getType(),
      updatedValues.getMaterial(),
      updatedValues.getSupplierMaterial(),
      updatedValues.getLaborRole(),
      updatedValues.getToolRental(),
      updatedValues.getDescription(),
      updatedValues.getUnitLabel(),
      updatedValues.getQuantity(),
      updatedValues.getCustomUnitCost(),
      updatedValues.getSortOrder());
    return map(composition,
      tenantId);
  }

  @Transactional
  public void deleteComponent(UUID compositionId,
    UUID componentId,
    UUID tenantId) {
    Composition composition = find(compositionId,
      tenantId);
    findComponent(composition,
      componentId);
    composition.removeComponent(componentId);
  }

  @Transactional(readOnly = true)
  public Response get(UUID id,
    UUID tenantId) {
    return map(find(id,
        tenantId),
      tenantId);
  }

  @Transactional(readOnly = true)
  public Page<Response> list(UUID tenantId,
    Boolean active,
    String search,
    Pageable pageable) {
    return compositions.search(tenantId,
      active,
      search == null ? "" : search,
      pageable).map(composition -> map(find(composition.getId(),
        tenantId),
      tenantId));
  }

  @Transactional
  public void status(UUID id,
    UUID tenantId,
    boolean active) {
    Composition composition = find(id,
      tenantId);
    composition.update(composition.getCode(),
      composition.getName(),
      composition.getDescription(),
      composition.getUnitOfMeasure(),
      composition.getBaseYield(),
      active);
  }

  public Composition find(UUID id,
    UUID tenantId) {
    return compositions.findDetailedByIdAndCompanyId(id,
      tenantId).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
      "Composition not found"));
  }

  public Response map(Composition composition,
    UUID tenantId) {
    List<ComponentResponse> lines = composition.getComponents().stream().map(compositionComponent -> line(compositionComponent,
      tenantId)).toList();
    BigDecimal total = lines.stream().map(ComponentResponse::totalCost).reduce(BigDecimal.ZERO,
      BigDecimal::add);
    return new Response(composition.getId(),
      composition.getCode(),
      composition.getName(),
      composition.getDescription(),
      composition.getUnitOfMeasure().getId(),
      composition.getUnitOfMeasure().getSymbol(),
      composition.getBaseYield(),
      composition.isActive(),
      lines,
      total,
      total.divide(composition.getBaseYield(),
        4,
        RoundingMode.HALF_UP));
  }

  private Response save(Composition composition,
    Request request,
    UUID tenantId) {
    if (compositions.existsByCompanyIdAndCodeAndIdNot(tenantId,
      request.code().trim(),
      composition.getId() == null ? new UUID(0,
        0) : composition.getId())) throw error(HttpStatus.CONFLICT,
      "Composition code already registered");
    UnitOfMeasure unitOfMeasure = units.findById(request.unitOfMeasureId()).filter(UnitOfMeasure::isActive).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
      "Active unit of measure not found"));
    List<CompositionComponent> lines = new ArrayList<>();
    for (int index = 0; index < request.components().size(); index++)
      lines.add(build(request.components().get(index),
        tenantId,
        index));
    composition.update(request.code().trim(),
      request.name().trim(),
      request.description(),
      unitOfMeasure,
      request.baseYield(),
      request.active() == null || request.active());
    composition.replaceComponents(lines);
    compositions.save(composition);
    return map(composition,
      tenantId);
  }

  private CompositionComponent build(ComponentRequest request,
    UUID tenantId,
    int index) {
    int sortOrder = request.sortOrder() == null ? index : request.sortOrder();
    return switch (request.type()) {
      case MATERIAL -> {
        if (request.materialId() == null || request.supplierMaterialId() == null) throw error(HttpStatus.BAD_REQUEST,
          "Material and supplier price are required");
        Material material = materials.findByIdAndCompanyId(request.materialId(),
          tenantId).filter(Material::isActive).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
          "Active material not found"));
        SupplierMaterial supplierMaterial = suppliers.findByIdAndMaterialIdAndMaterialCompanyId(request.supplierMaterialId(),
          material.getId(),
          tenantId).filter(candidateSupplierMaterial -> candidateSupplierMaterial.getReferencePrice() != null).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
          "Material supplier price not found"));
        yield new CompositionComponent(request.type(),
          material,
          supplierMaterial,
          null,
          null,
          null,
          null,
          request.quantity(),
          null,
          sortOrder);
      }
      case LABOR -> {
        if (request.laborRoleId() == null) throw error(HttpStatus.BAD_REQUEST,
          "Labor role is required");
        LaborRole laborRole = labor.find(request.laborRoleId(),
          tenantId);
        if (!laborRole.isActive()) throw error(HttpStatus.NOT_FOUND,
          "Active labor role not found");
        labor.hourlyCost(laborRole,
          tenantId);
        yield new CompositionComponent(request.type(),
          null,
          null,
          laborRole,
          null,
          null,
          null,
          request.quantity(),
          null,
          sortOrder);
      }
      case EQUIPMENT -> {
        if (request.toolRentalId() == null) throw error(HttpStatus.BAD_REQUEST,
          "Tool rental is required");
        ToolRental toolRental = rentals.findByIdAndCompanyId(request.toolRentalId(),
          tenantId).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
          "Tool rental not found"));
        yield new CompositionComponent(request.type(),
          null,
          null,
          null,
          toolRental,
          null,
          null,
          request.quantity(),
          null,
          sortOrder);
      }
      case CUSTOM -> {
        if (request.description() == null || request.description().isBlank() || request.unitLabel() == null || request.unitLabel().isBlank() || request.unitCost() == null)
          throw error(HttpStatus.BAD_REQUEST,
            "Custom description, unit and cost are required");
        yield new CompositionComponent(request.type(),
          null,
          null,
          null,
          null,
          request.description().trim(),
          request.unitLabel().trim(),
          request.quantity(),
          request.unitCost(),
          sortOrder);
      }
    };
  }

  private ComponentResponse line(CompositionComponent compositionComponent,
    UUID tenantId) {
    BigDecimal cost;
    String name;
    String unit;
    UUID referenceId;
    switch (compositionComponent.getType()) {
      case MATERIAL -> {
        cost = compositionComponent.getSupplierMaterial().getReferencePrice();
        name = compositionComponent.getMaterial().getName();
        unit = compositionComponent.getMaterial().getUnitOfMeasure().getSymbol();
        referenceId = compositionComponent.getSupplierMaterial().getId();
      }
      case LABOR -> {
        cost = labor.hourlyCost(compositionComponent.getLaborRole(),
          tenantId);
        name = compositionComponent.getLaborRole().getName();
        unit = "h";
        referenceId = compositionComponent.getLaborRole().getId();
      }
      case EQUIPMENT -> {
        cost = compositionComponent.getToolRental().getPrice();
        name = compositionComponent.getToolRental().getTool().getName();
        unit = compositionComponent.getToolRental().getRentalPeriod().name();
        referenceId = compositionComponent.getToolRental().getId();
      }
      default -> {
        cost = compositionComponent.getCustomUnitCost();
        name = compositionComponent.getDescription();
        unit = compositionComponent.getUnitLabel();
        referenceId = null;
      }
    }
    return new ComponentResponse(compositionComponent.getId(),
      compositionComponent.getType(),
      referenceId,
      name,
      unit,
      compositionComponent.getQuantity(),
      cost,
      cost.multiply(compositionComponent.getQuantity()).setScale(4,
        RoundingMode.HALF_UP),
      compositionComponent.getSortOrder());
  }

  private CompositionComponent findComponent(Composition composition,
    UUID componentId) {
    return composition.getComponents().stream()
      .filter(compositionComponent -> compositionComponent.getId().equals(componentId))
      .findFirst()
      .orElseThrow(() -> error(HttpStatus.NOT_FOUND,
        "Composition component not found"));
  }

  private ResponseStatusException error(HttpStatus status,
    String message) {
    return new ResponseStatusException(status,
      message);
  }
}
