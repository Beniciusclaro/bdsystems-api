package com.bdsystems.bdsystems_api.materials.domains;

import com.bdsystems.bdsystems_api.materials.domains.dtos.MaterialCategoryResponse;
import com.bdsystems.bdsystems_api.materials.domains.dtos.MaterialResponse;
import com.bdsystems.bdsystems_api.materials.domains.dtos.SupplierMaterialResponse;
import com.bdsystems.bdsystems_api.materials.domains.dtos.UnitOfMeasureResponse;
import com.bdsystems.bdsystems_api.materials.domains.entities.Material;
import com.bdsystems.bdsystems_api.materials.domains.entities.MaterialCategory;
import com.bdsystems.bdsystems_api.materials.domains.entities.SupplierMaterial;
import com.bdsystems.bdsystems_api.materials.domains.entities.UnitOfMeasure;

public final class MaterialMapper {
  private MaterialMapper() {
  }

  public static UnitOfMeasureResponse toResponse(UnitOfMeasure u) {
    return new UnitOfMeasureResponse(u.getId(),
      u.getCode(),
      u.getName(),
      u.getSymbol(),
      u.isActive());
  }

  public static MaterialCategoryResponse toResponse(MaterialCategory c) {
    return new MaterialCategoryResponse(c.getId(),
      c.getCompany().getId(),
      c.getName(),
      c.getDescription(),
      c.isActive());
  }

  public static MaterialResponse toResponse(Material m) {
    return new MaterialResponse(m.getId(),
      m.getCompany().getId(),
      m.getCode(),
      m.getName(),
      m.getDescription(),
      toResponse(m.getUnitOfMeasure()),
      m.getCategory() == null ? null : toResponse(m.getCategory()),
      m.isActive(),
      m.getCreatedAt(),
      m.getUpdatedAt());
  }

  public static SupplierMaterialResponse toResponse(SupplierMaterial s) {
    return new SupplierMaterialResponse(s.getId(),
      s.getMaterial().getId(),
      s.getSupplier().getId(),
      s.getSupplier().getLegalName(),
      s.getSupplierCode(),
      s.getReferencePrice(),
      s.getCreatedAt(),
      s.getUpdatedAt());
  }
}
