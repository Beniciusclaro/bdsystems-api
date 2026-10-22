package com.bdsystems.bdsystems_api.materials.services;

import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.materials.domains.MaterialMapper;
import com.bdsystems.bdsystems_api.materials.domains.dtos.*;
import com.bdsystems.bdsystems_api.materials.domains.entities.Material;
import com.bdsystems.bdsystems_api.materials.domains.entities.MaterialCategory;
import com.bdsystems.bdsystems_api.materials.domains.entities.SupplierMaterial;
import com.bdsystems.bdsystems_api.materials.repositories.MaterialCategoryRepository;
import com.bdsystems.bdsystems_api.materials.repositories.MaterialRepository;
import com.bdsystems.bdsystems_api.materials.repositories.SupplierMaterialRepository;
import com.bdsystems.bdsystems_api.materials.repositories.UnitOfMeasureRepository;
import com.bdsystems.bdsystems_api.supplyer.repositories.SupplierRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Validated
@Transactional(readOnly = true)
public class MaterialService {
  private final MaterialRepository materials;
  private final MaterialCategoryRepository categories;
  private final UnitOfMeasureRepository units;
  private final SupplierMaterialRepository associations;
  private final SupplierRepository suppliers;
  private final CompanyRepository companies;

  public MaterialService(MaterialRepository materials,
    MaterialCategoryRepository categories,
    UnitOfMeasureRepository units,
    SupplierMaterialRepository associations,
    SupplierRepository suppliers,
    CompanyRepository companies) {
    this.materials = materials;
    this.categories = categories;
    this.units = units;
    this.associations = associations;
    this.suppliers = suppliers;
    this.companies = companies;
  }

  @Transactional
  public MaterialResponse create(@NotNull @Valid MaterialRequest request,
    UUID companyId) {
    var company = companies.findById(companyId).orElseThrow(() -> error(HttpStatus.FORBIDDEN,
      "Company not found"));
    Material material = new Material(company);
    apply(material,
      request,
      companyId);
    materials.saveAndFlush(material);
    upsertSuppliers(material,
      request.suppliers(),
      companyId);
    return MaterialMapper.toResponse(material);
  }

  @Transactional
  public MaterialResponse update(UUID id,
    @NotNull @Valid MaterialRequest request,
    UUID companyId) {
    Material material = findMaterial(id,
      companyId);
    apply(material,
      request,
      companyId);
    upsertSuppliers(material,
      request.suppliers(),
      companyId);
    return MaterialMapper.toResponse(materials.saveAndFlush(material));
  }

  private void apply(Material material,
    MaterialRequest request,
    UUID companyId) {
    String code = request.code().trim();
    if (materials.existsByCompanyIdAndCodeAndIdNot(companyId,
      code,
      material.getId() == null ? new UUID(0,
        0) : material.getId())) {
      throw error(HttpStatus.CONFLICT,
        "Material code already registered for this company");
    }
    var unit = units.findById(request.unitOfMeasureId()).orElseThrow(() -> missing("Unit of measure"));
    var category = request.categoryId() == null ? null : findCategory(request.categoryId(),
      companyId);
    material.updateDetails(code,
      request.name().trim(),
      request.description(),
      unit,
      category,
      request.active() == null ? material.isActive() : request.active());
  }

  // Omitted suppliers are preserved. Supplied entries are added or updated; removal is explicit.
  private void upsertSuppliers(Material material,
    List<SupplierMaterialRequest> requests,
    UUID companyId) {
    if (requests == null) return;
    Set<UUID> seen = new HashSet<>();
    for (var request : requests) {
      if (!seen.add(request.supplierId())) throw error(HttpStatus.BAD_REQUEST,
        "Duplicate supplier in request");
      var supplier = suppliers.findByIdAndCompanyIdAndActiveTrue(request.supplierId(),
          companyId)
        .orElseThrow(() -> missing("Active supplier"));
      var association = associations.findByMaterialIdAndSupplierId(material.getId(),
          supplier.getId())
        .orElseGet(() -> new SupplierMaterial(material,
          supplier));
      association.updateDetails(trim(request.supplierCode()),
        request.referencePrice());
      associations.saveAndFlush(association);
    }
  }

  public MaterialResponse findById(UUID id,
    UUID companyId) {
    return MaterialMapper.toResponse(findMaterial(id,
      companyId));
  }

  public Page<MaterialResponse> findAll(UUID companyId,
    Boolean active,
    UUID categoryId,
    String name,
    String code,
    Pageable pageable) {
    Boolean activeFilter = active == null ? Boolean.TRUE : active;
    return materials.findByFilters(companyId,
        activeFilter,
        categoryId,
        filter(name),
        filter(code),
        pageable)
      .map(MaterialMapper::toResponse);
  }

  @Transactional
  public void changeStatus(UUID id,
    UUID companyId,
    boolean active) {
    findMaterial(id,
      companyId).changeStatus(active);
  }

  public Page<SupplierMaterialResponse> findSuppliers(UUID id,
    UUID companyId,
    Pageable pageable) {
    findMaterial(id,
      companyId);
    return associations.findByMaterialIdAndMaterialCompanyId(id,
      companyId,
      pageable).map(MaterialMapper::toResponse);
  }

  @Transactional
  public SupplierMaterialResponse addSupplier(UUID id,
    @NotNull @Valid SupplierMaterialRequest request,
    UUID companyId) {
    var material = findMaterial(id,
      companyId);
    if (associations.existsByMaterialIdAndSupplierId(id,
      request.supplierId())) {
      throw error(HttpStatus.CONFLICT,
        "Supplier already associated with material");
    }
    var supplier = suppliers.findByIdAndCompanyIdAndActiveTrue(request.supplierId(),
        companyId)
      .orElseThrow(() -> missing("Active supplier"));
    var association = new SupplierMaterial(material,
      supplier);
    association.updateDetails(trim(request.supplierCode()),
      request.referencePrice());
    return MaterialMapper.toResponse(associations.saveAndFlush(association));
  }

  @Transactional
  public SupplierMaterialResponse updateSupplier(UUID id,
    UUID associationId,
    @NotNull @Valid SupplierMaterialRequest request,
    UUID companyId) {
    var association = findAssociation(id,
      associationId,
      companyId);
    if (!association.getSupplier().getId().equals(request.supplierId())) {
      throw error(HttpStatus.BAD_REQUEST,
        "Supplier cannot be changed; remove and add the association instead");
    }
    association.updateDetails(trim(request.supplierCode()),
      request.referencePrice());
    return MaterialMapper.toResponse(associations.saveAndFlush(association));
  }

  @Transactional
  public void removeSupplier(UUID id,
    UUID associationId,
    UUID companyId) {
    associations.delete(findAssociation(id,
      associationId,
      companyId));
  }

  private SupplierMaterial findAssociation(UUID id,
    UUID associationId,
    UUID companyId) {
    return associations.findByIdAndMaterialIdAndMaterialCompanyId(associationId,
        id,
        companyId)
      .orElseThrow(() -> missing("Supplier association"));
  }

  @Transactional
  public MaterialCategoryResponse createCategory(@NotNull @Valid MaterialCategoryRequest request,
    UUID companyId) {
    validateCategoryName(request.name().trim(),
      companyId,
      new UUID(0,
        0));
    var company = companies.findById(companyId).orElseThrow(() -> error(HttpStatus.FORBIDDEN,
      "Company not found"));
    return MaterialMapper.toResponse(categories.saveAndFlush(new MaterialCategory(company,
      request.name().trim(),
      request.description(),
      request.active() == null || request.active())));
  }

  @Transactional
  public MaterialCategoryResponse updateCategory(UUID id,
    @NotNull @Valid MaterialCategoryRequest request,
    UUID companyId) {
    var category = findCategory(id,
      companyId);
    validateCategoryName(request.name().trim(),
      companyId,
      id);
    category.updateDetails(request.name().trim(),
      request.description(),
      request.active() == null ? category.isActive() : request.active());
    return MaterialMapper.toResponse(categories.saveAndFlush(category));
  }

  public Page<MaterialCategoryResponse> findCategories(UUID companyId,
    Boolean active,
    Pageable pageable) {
    return categories.findByFilters(companyId,
      active,
      pageable).map(MaterialMapper::toResponse);
  }

  @Transactional
  public void changeCategoryStatus(UUID id,
    UUID companyId,
    boolean active) {
    findCategory(id,
      companyId).changeStatus(active);
  }

  public Page<UnitOfMeasureResponse> findUnits(boolean active,
    Pageable pageable) {
    return units.findByActive(active,
      pageable).map(MaterialMapper::toResponse);
  }

  private void validateCategoryName(String name,
    UUID companyId,
    UUID id) {
    if (categories.existsByCompanyIdAndNameAndIdNot(companyId,
      name,
      id)) {
      throw error(HttpStatus.CONFLICT,
        "Category name already registered for this company");
    }
  }

  private Material findMaterial(UUID id,
    UUID companyId) {
    return materials.findByIdAndCompanyId(id,
      companyId).orElseThrow(() -> missing("Material"));
  }

  private MaterialCategory findCategory(UUID id,
    UUID companyId) {
    return categories.findByIdAndCompanyId(id,
      companyId).orElseThrow(() -> missing("Material category"));
  }

  private String trim(String value) {
    return value == null ? null : value.trim();
  }

  private String filter(String value) {
    return value == null ? "" : value.trim();
  }

  private ResponseStatusException missing(String resource) {
    return error(HttpStatus.NOT_FOUND,
      resource + " not found");
  }

  private ResponseStatusException error(HttpStatus status,
    String message) {
    return new ResponseStatusException(status,
      message);
  }
}
