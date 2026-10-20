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

  public CompositionService(CompositionRepository c,
    CompanyRepository co,
    UnitOfMeasureRepository u,
    MaterialRepository m,
    SupplierMaterialRepository s,
    ToolRentalRepository r,
    LaborService l) {
    compositions = c;
    companies = co;
    units = u;
    materials = m;
    suppliers = s;
    rentals = r;
    labor = l;
  }

  @Transactional
  public Response create(Request r,
    UUID t) {
    return save(new Composition(companies.findById(t).orElseThrow(() -> error(HttpStatus.FORBIDDEN,
        "Authenticated company not found"))),
      r,
      t);
  }

  @Transactional
  public Response update(UUID id,
    Request r,
    UUID t) {
    return save(find(id,
        t),
      r,
      t);
  }

  @Transactional(readOnly = true)
  public Response get(UUID id,
    UUID t) {
    return map(find(id,
        t),
      t);
  }

  @Transactional(readOnly = true)
  public Page<Response> list(UUID t,
    Boolean active,
    String search,
    Pageable p) {
    return compositions.search(t,
      active,
      search == null ? "" : search,
      p).map(c -> map(find(c.getId(),
        t),
      t));
  }

  @Transactional
  public void status(UUID id,
    UUID t,
    boolean active) {
    Composition c = find(id,
      t);
    c.update(c.getCode(),
      c.getName(),
      c.getDescription(),
      c.getUnitOfMeasure(),
      c.getBaseYield(),
      active);
  }

  public Composition find(UUID id,
    UUID t) {
    return compositions.findDetailedByIdAndCompanyId(id,
      t).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
      "Composition not found"));
  }

  public Response map(Composition c,
    UUID t) {
    List<ComponentResponse> lines = c.getComponents().stream().map(x -> line(x,
      t)).toList();
    BigDecimal total = lines.stream().map(ComponentResponse::totalCost).reduce(BigDecimal.ZERO,
      BigDecimal::add);
    return new Response(c.getId(),
      c.getCode(),
      c.getName(),
      c.getDescription(),
      c.getUnitOfMeasure().getId(),
      c.getUnitOfMeasure().getSymbol(),
      c.getBaseYield(),
      c.isActive(),
      lines,
      total,
      total.divide(c.getBaseYield(),
        4,
        RoundingMode.HALF_UP));
  }

  private Response save(Composition c,
    Request r,
    UUID t) {
    if (compositions.existsByCompanyIdAndCodeAndIdNot(t,
      r.code().trim(),
      c.getId() == null ? new UUID(0,
        0) : c.getId())) throw error(HttpStatus.CONFLICT,
      "Composition code already registered");
    UnitOfMeasure u = units.findById(r.unitOfMeasureId()).filter(UnitOfMeasure::isActive).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
      "Active unit of measure not found"));
    List<CompositionComponent> lines = new ArrayList<>();
    for (int i = 0; i < r.components().size(); i++)
      lines.add(build(r.components().get(i),
        t,
        i));
    c.update(r.code().trim(),
      r.name().trim(),
      r.description(),
      u,
      r.baseYield(),
      r.active() == null || r.active());
    c.replaceComponents(lines);
    compositions.save(c);
    return map(c,
      t);
  }

  private CompositionComponent build(ComponentRequest r,
    UUID t,
    int index) {
    int order = r.sortOrder() == null ? index : r.sortOrder();
    return switch (r.type()) {
      case MATERIAL -> {
        if (r.materialId() == null || r.supplierMaterialId() == null) throw error(HttpStatus.BAD_REQUEST,
          "Material and supplier price are required");
        Material m = materials.findByIdAndCompanyId(r.materialId(),
          t).filter(Material::isActive).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
          "Active material not found"));
        SupplierMaterial sm = suppliers.findByIdAndMaterialIdAndMaterialCompanyId(r.supplierMaterialId(),
          m.getId(),
          t).filter(x -> x.getReferencePrice() != null).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
          "Material supplier price not found"));
        yield new CompositionComponent(r.type(),
          m,
          sm,
          null,
          null,
          null,
          null,
          r.quantity(),
          null,
          order);
      }
      case LABOR -> {
        if (r.laborRoleId() == null) throw error(HttpStatus.BAD_REQUEST,
          "Labor role is required");
        LaborRole lr = labor.find(r.laborRoleId(),
          t);
        if (!lr.isActive()) throw error(HttpStatus.NOT_FOUND,
          "Active labor role not found");
        labor.hourlyCost(lr,
          t);
        yield new CompositionComponent(r.type(),
          null,
          null,
          lr,
          null,
          null,
          null,
          r.quantity(),
          null,
          order);
      }
      case EQUIPMENT -> {
        if (r.toolRentalId() == null) throw error(HttpStatus.BAD_REQUEST,
          "Tool rental is required");
        ToolRental tr = rentals.findByIdAndCompanyId(r.toolRentalId(),
          t).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
          "Tool rental not found"));
        yield new CompositionComponent(r.type(),
          null,
          null,
          null,
          tr,
          null,
          null,
          r.quantity(),
          null,
          order);
      }
      case CUSTOM -> {
        if (r.description() == null || r.description().isBlank() || r.unitLabel() == null || r.unitLabel().isBlank() || r.unitCost() == null)
          throw error(HttpStatus.BAD_REQUEST,
            "Custom description, unit and cost are required");
        yield new CompositionComponent(r.type(),
          null,
          null,
          null,
          null,
          r.description().trim(),
          r.unitLabel().trim(),
          r.quantity(),
          r.unitCost(),
          order);
      }
    };
  }

  private ComponentResponse line(CompositionComponent x,
    UUID t) {
    BigDecimal cost;
    String name;
    String unit;
    UUID ref;
    switch (x.getType()) {
      case MATERIAL -> {
        cost = x.getSupplierMaterial().getReferencePrice();
        name = x.getMaterial().getName();
        unit = x.getMaterial().getUnitOfMeasure().getSymbol();
        ref = x.getSupplierMaterial().getId();
      }
      case LABOR -> {
        cost = labor.hourlyCost(x.getLaborRole(),
          t);
        name = x.getLaborRole().getName();
        unit = "h";
        ref = x.getLaborRole().getId();
      }
      case EQUIPMENT -> {
        cost = x.getToolRental().getPrice();
        name = x.getToolRental().getTool().getName();
        unit = x.getToolRental().getRentalPeriod().name();
        ref = x.getToolRental().getId();
      }
      default -> {
        cost = x.getCustomUnitCost();
        name = x.getDescription();
        unit = x.getUnitLabel();
        ref = null;
      }
    }
    return new ComponentResponse(x.getId(),
      x.getType(),
      ref,
      name,
      unit,
      x.getQuantity(),
      cost,
      cost.multiply(x.getQuantity()).setScale(4,
        RoundingMode.HALF_UP),
      x.getSortOrder());
  }

  private ResponseStatusException error(HttpStatus s,
    String m) {
    return new ResponseStatusException(s,
      m);
  }
}
