package com.bdsystems.bdsystems_api.budgets.services;

import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.ComponentResponse;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.ApprovalRequest;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.ApprovalResponse;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.ItemRequest;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.ItemResponse;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.Proposal;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.ProposalItem;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.ProposalStage;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.QuantityRequest;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.Request;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.Response;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.StageRequest;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.StageResponse;
import com.bdsystems.bdsystems_api.budgets.domains.entities.Budget;
import com.bdsystems.bdsystems_api.budgets.domains.entities.BudgetItem;
import com.bdsystems.bdsystems_api.budgets.domains.entities.BudgetItemComponent;
import com.bdsystems.bdsystems_api.budgets.domains.entities.BudgetStage;
import com.bdsystems.bdsystems_api.budgets.domains.entities.BudgetStatus;
import com.bdsystems.bdsystems_api.budgets.domains.entities.ProjectLinkOrigin;
import com.bdsystems.bdsystems_api.budgets.repositories.BudgetItemRepository;
import com.bdsystems.bdsystems_api.budgets.repositories.BudgetRepository;
import com.bdsystems.bdsystems_api.budgets.repositories.BudgetStageRepository;
import com.bdsystems.bdsystems_api.clients.domains.entities.Client;
import com.bdsystems.bdsystems_api.clients.repositories.ClientRepository;
import com.bdsystems.bdsystems_api.compositions.domains.dtos.CompositionDtos;
import com.bdsystems.bdsystems_api.compositions.domains.entities.Composition;
import com.bdsystems.bdsystems_api.compositions.services.CompositionService;
import com.bdsystems.bdsystems_api.projects.domains.entities.Project;
import com.bdsystems.bdsystems_api.projects.repositories.ProjectRepository;
import com.bdsystems.bdsystems_api.projects.services.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class BudgetService {
  private static final BigDecimal HUNDRED = new BigDecimal("100");
  private final BudgetRepository budgets;
  private final BudgetStageRepository stages;
  private final BudgetItemRepository items;
  private final ClientRepository clients;
  private final ProjectRepository projects;
  private final CompanyRepository companies;
  private final CompositionService compositions;
  private final ProjectService projectService;

  public BudgetService(BudgetRepository b,
    BudgetStageRepository s,
    BudgetItemRepository i,
    ClientRepository c,
    ProjectRepository p,
    CompanyRepository co,
    CompositionService cs,
    ProjectService projectService) {
    budgets = b;
    stages = s;
    items = i;
    clients = c;
    projects = p;
    companies = co;
    compositions = cs;
    this.projectService = projectService;
  }

  @Transactional
  public Response create(Request r,
    UUID t) {
    return save(new Budget(companies.findById(t).orElseThrow(() -> error(HttpStatus.FORBIDDEN,
        "Authenticated company not found"))),
      r,
      t);
  }

  @Transactional
  public Response update(UUID id,
    Request r,
    UUID t) {
    Budget b = find(id,
      t);
    editable(b);
    return save(b,
      r,
      t);
  }

  @Transactional(readOnly = true)
  public Response get(UUID id,
    UUID t) {
    return map(find(id,
      t));
  }

  @Transactional(readOnly = true)
  public Proposal proposal(UUID id,
    UUID t) {
    Budget b = find(id,
      t);
    String client = b.getStatus() == BudgetStatus.DRAFT ? b.getClient().getName() : b.getClientNameSnapshot();
    String project = b.getStatus() == BudgetStatus.DRAFT ? b.getProposedProjectName() : b.getProjectNameSnapshot();
    List<ProposalStage> result = b.getStages().stream().map(s -> new ProposalStage(s.getName(),
      s.getDescription(),
      s.getItems().stream().map(BudgetItem::getTotalSalePrice).reduce(BigDecimal.ZERO,
        BigDecimal::add),
      s.getItems().stream().map(i -> new ProposalItem(i.getName(),
        i.getDescription(),
        i.getUnitSymbol(),
        i.getQuantity(),
        i.getUnitSalePrice(),
        i.getTotalSalePrice())).toList())).toList();
    return new Proposal(b.getNumber(),
      b.getTitle(),
      client,
      project,
      b.getValidUntil(),
      b.getCommercialTerms(),
      b.getNotes(),
      result.stream().map(ProposalStage::total).reduce(BigDecimal.ZERO,
        BigDecimal::add),
      result);
  }

  @Transactional(readOnly = true)
  public Page<Response> list(UUID t,
    Pageable p) {
    return budgets.findByCompanyId(t,
      p).map(b -> map(find(b.getId(),
      t)));
  }

  @Transactional
  public Response addStage(UUID id,
    StageRequest r,
    UUID t) {
    Budget b = find(id,
      t);
    editable(b);
    b.addStage(new BudgetStage(r.name().trim(),
      r.description(),
      r.sortOrder() == null ? b.getStages().size() : r.sortOrder()));
    return map(budgets.save(b));
  }

  @Transactional
  public Response updateStage(UUID budgetId,
    UUID stageId,
    StageRequest r,
    UUID t) {
    Budget b = find(budgetId,
      t);
    editable(b);
    BudgetStage s = stage(stageId,
      budgetId,
      t);
    s.update(r.name().trim(),
      r.description(),
      r.sortOrder() == null ? s.getSortOrder() : r.sortOrder());
    return map(b);
  }

  @Transactional
  public Response deleteStage(UUID budgetId,
    UUID stageId,
    UUID t) {
    Budget b = find(budgetId,
      t);
    editable(b);
    BudgetStage s = stage(stageId,
      budgetId,
      t);
    b.getStages().remove(s);
    stages.delete(s);
    return map(b);
  }

  @Transactional
  public Response addItem(UUID budgetId,
    UUID stageId,
    ItemRequest r,
    UUID t) {
    Budget b = find(budgetId,
      t);
    editable(b);
    BudgetStage s = stage(stageId,
      budgetId,
      t);
    Composition c = compositions.find(r.compositionId(),
      t);
    if (!c.isActive()) throw error(HttpStatus.NOT_FOUND,
      "Active composition not found");
    CompositionDtos.Response cr = compositions.map(c,
      t);
    BudgetItem item = new BudgetItem(c,
      c.getCode(),
      c.getName(),
      c.getDescription(),
      c.getUnitOfMeasure().getSymbol(),
      r.quantity(),
      cr.unitCost(),
      r.sortOrder() == null ? s.getItems().size() : r.sortOrder());
    item.replaceComponents(snapshot(cr));
    item.recalculate(denominator(b));
    s.addItem(item);
    return map(budgets.save(b));
  }

  @Transactional
  public Response quantity(UUID budgetId,
    UUID itemId,
    QuantityRequest r,
    UUID t) {
    Budget b = find(budgetId,
      t);
    editable(b);
    item(itemId,
      budgetId,
      t).changeQuantity(r.quantity(),
      denominator(b));
    return map(b);
  }

  @Transactional
  public Response refresh(UUID budgetId,
    UUID itemId,
    UUID t) {
    Budget b = find(budgetId,
      t);
    editable(b);
    BudgetItem i = item(itemId,
      budgetId,
      t);
    if (i.getSourceComposition() == null) throw error(HttpStatus.UNPROCESSABLE_ENTITY,
      "Item has no source composition");
    Composition c = compositions.find(i.getSourceComposition().getId(),
      t);
    CompositionDtos.Response cr = compositions.map(c,
      t);
    i.refresh(c,
      c.getCode(),
      c.getName(),
      c.getDescription(),
      c.getUnitOfMeasure().getSymbol(),
      cr.unitCost(),
      denominator(b));
    i.replaceComponents(snapshot(cr));
    return map(b);
  }

  @Transactional
  public Response deleteItem(UUID budgetId,
    UUID itemId,
    UUID t) {
    Budget b = find(budgetId,
      t);
    editable(b);
    BudgetItem i = item(itemId,
      budgetId,
      t);
    i.getStage().getItems().remove(i);
    items.delete(i);
    return map(b);
  }

  @Transactional
  public Response finalizeBudget(UUID id,
    UUID t) {
    Budget b = find(id,
      t);
    editable(b);
    if (b.getStages().isEmpty() || b.getStages().stream().allMatch(s -> s.getItems().isEmpty()))
      throw error(HttpStatus.UNPROCESSABLE_ENTITY,
        "Budget must contain at least one service");
    b.finalizeBudget();
    return map(b);
  }

  @Transactional
  public ApprovalResponse approveAndCreateProject(UUID id,
    ApprovalRequest request,
    UUID tenantId) {
    Budget budget = find(id,
      tenantId);
    requireFinalized(budget);
    Project project;
    ProjectLinkOrigin linkOrigin;
    if (budget.getProject() != null && budget.getProjectLinkOrigin() == ProjectLinkOrigin.LEGACY) {
      project = projects.findByIdAndCompanyId(budget.getProject().getId(),
        tenantId).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
        "Project not found"));
      linkOrigin = ProjectLinkOrigin.LEGACY;
    } else {
      if (budget.getProject() != null) throw error(HttpStatus.CONFLICT,
        "Budget already has a generated project");
      project = projectService.createFromBudget(budget.getClient(),
        request.code(),
        request.name(),
        request.description(),
        request.street(),
        request.city(),
        request.state(),
        request.zipCode(),
        request.plannedStartDate(),
        request.plannedEndDate(),
        tenantId);
      linkOrigin = ProjectLinkOrigin.GENERATED;
    }
    budget.approve(project,
      linkOrigin);
    return new ApprovalResponse(map(budget),
      projectService.map(project));
  }

  @Transactional
  public Response reject(UUID id,
    UUID tenantId) {
    Budget budget = find(id,
      tenantId);
    requireFinalized(budget);
    budget.reject();
    return map(budget);
  }

  @Transactional
  public Response duplicate(UUID id,
    String newNumber,
    UUID t) {
    Budget source = find(id,
      t);
    if (newNumber == null || newNumber.isBlank()) throw error(HttpStatus.BAD_REQUEST,
      "New budget number is required");
    Request r = new Request(source.getClient().getId(),
      newNumber,
      source.getTitle() + " - revisão",
      source.getValidUntil(),
      source.getCommercialTerms(),
      source.getNotes(),
      source.getTaxPercent(),
      source.getMarginPercent(),
      source.getProposedProjectName(),
      source.getProposedProjectDescription(),
      source.getProposedProjectStreet(),
      source.getProposedProjectCity(),
      source.getProposedProjectState(),
      source.getProposedProjectZipCode());
    Budget copy = new Budget(companies.findById(t).orElseThrow());
    apply(copy,
      r,
      t);
    for (BudgetStage oldStage : source.getStages()) {
      BudgetStage ns = new BudgetStage(oldStage.getName(),
        oldStage.getDescription(),
        oldStage.getSortOrder());
      copy.addStage(ns);
      for (BudgetItem oi : oldStage.getItems()) {
        BudgetItem ni = new BudgetItem(oi.getSourceComposition(),
          oi.getCompositionCodeSnapshot(),
          oi.getName(),
          oi.getDescription(),
          oi.getUnitSymbol(),
          oi.getQuantity(),
          oi.getUnitCost(),
          oi.getSortOrder());
        List<BudgetItemComponent> cs = oi.getComponents().stream().map(x -> new BudgetItemComponent(x.getType(),
          x.getDescription(),
          x.getUnitLabel(),
          x.getQuantity(),
          x.getUnitCost(),
          x.getTotalCost(),
          x.getSourceReferenceId(),
          x.getSortOrder())).toList();
        ni.replaceComponents(cs);
        ni.recalculate(denominator(copy));
        ns.addItem(ni);
      }
    }
    return map(budgets.save(copy));
  }

  private Response save(Budget b,
    Request r,
    UUID t) {
    apply(b,
      r,
      t);
    b.getStages().forEach(s -> s.getItems().forEach(i -> i.recalculate(denominator(b))));
    return map(budgets.save(b));
  }

  private void apply(Budget b,
    Request r,
    UUID t) {
    validatePercents(r.taxPercent(),
      r.marginPercent());
    if (budgets.existsByCompanyIdAndNumberAndIdNot(t,
      r.number().trim(),
      b.getId() == null ? new UUID(0,
        0) : b.getId())) throw error(HttpStatus.CONFLICT,
      "Budget number already registered");
    Client c = clients.findByIdAndCompanyId(r.clientId(),
      t).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
      "Client not found"));
    b.update(c,
      r.number().trim(),
      r.title().trim(),
      r.validUntil(),
      r.commercialTerms(),
      r.notes(),
      r.taxPercent(),
      r.marginPercent(),
      r.proposedProjectName().trim(),
      r.proposedProjectDescription(),
      r.proposedProjectStreet(),
      r.proposedProjectCity(),
      r.proposedProjectState(),
      r.proposedProjectZipCode());
  }

  private List<BudgetItemComponent> snapshot(CompositionDtos.Response r) {
    return r.components().stream().map(x -> new BudgetItemComponent(x.type(),
      x.description(),
      x.unitLabel(),
      x.quantity(),
      x.unitCost(),
      x.totalCost(),
      x.referenceId(),
      x.sortOrder())).toList();
  }

  private void validatePercents(BigDecimal tax,
    BigDecimal margin) {
    if (tax.add(margin).compareTo(HUNDRED) >= 0) throw error(HttpStatus.BAD_REQUEST,
      "Tax plus margin must be less than 100 percent");
  }

  private BigDecimal denominator(Budget b) {
    return BigDecimal.ONE.subtract(b.getTaxPercent().add(b.getMarginPercent()).divide(HUNDRED,
      8,
      RoundingMode.HALF_UP));
  }

  Budget find(UUID id,
    UUID t) {
    return budgets.findDetailedByIdAndCompanyId(id,
      t).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
      "Budget not found"));
  }

  private BudgetStage stage(UUID id,
    UUID b,
    UUID t) {
    return stages.findByIdAndBudgetIdAndBudgetCompanyId(id,
      b,
      t).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
      "Budget stage not found"));
  }

  private BudgetItem item(UUID id,
    UUID b,
    UUID t) {
    return items.findByIdAndStageBudgetIdAndStageBudgetCompanyId(id,
      b,
      t).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
      "Budget item not found"));
  }

  private void editable(Budget b) {
    if (b.getStatus() != BudgetStatus.DRAFT) throw error(HttpStatus.CONFLICT,
      "Only draft budgets can be changed");
  }

  private void requireFinalized(Budget budget) {
    if (budget.getStatus() != BudgetStatus.FINALIZED) throw error(HttpStatus.CONFLICT,
      "Only finalized budgets can be approved or rejected");
  }

  private Response map(Budget b) {
    List<StageResponse> ss = b.getStages().stream().map(s -> {
      List<ItemResponse> is = s.getItems().stream().map(i -> new ItemResponse(i.getId(),
        i.getSourceComposition() == null ? null : i.getSourceComposition().getId(),
        i.getCompositionCodeSnapshot(),
        i.getName(),
        i.getDescription(),
        i.getUnitSymbol(),
        i.getQuantity(),
        i.getUnitCost(),
        i.getTotalCost(),
        i.getUnitSalePrice(),
        i.getTotalSalePrice(),
        i.getSortOrder(),
        i.getComponents().stream().map(c -> new ComponentResponse(c.getType(),
          c.getDescription(),
          c.getUnitLabel(),
          c.getQuantity(),
          c.getUnitCost(),
          c.getTotalCost())).toList())).toList();
      BigDecimal cost = is.stream().map(ItemResponse::totalCost).reduce(BigDecimal.ZERO,
        BigDecimal::add);
      BigDecimal sale = is.stream().map(ItemResponse::totalSalePrice).reduce(BigDecimal.ZERO,
        BigDecimal::add);
      return new StageResponse(s.getId(),
        s.getName(),
        s.getDescription(),
        s.getSortOrder(),
        cost,
        sale,
        is);
    }).toList();
    BigDecimal cost = ss.stream().map(StageResponse::totalCost).reduce(BigDecimal.ZERO,
      BigDecimal::add);
    BigDecimal sale = ss.stream().map(StageResponse::totalSalePrice).reduce(BigDecimal.ZERO,
      BigDecimal::add);
    String clientName = b.getStatus() == BudgetStatus.DRAFT ? b.getClient().getName() : b.getClientNameSnapshot();
    String projectName = b.getProject() == null ? null : b.getProject().getName();
    return new Response(b.getId(),
      b.getNumber(),
      b.getTitle(),
      b.getClient().getId(),
      clientName,
      b.getProject() == null ? null : b.getProject().getId(),
      projectName,
      b.getProposedProjectName(),
      b.getProposedProjectDescription(),
      b.getProposedProjectStreet(),
      b.getProposedProjectCity(),
      b.getProposedProjectState(),
      b.getProposedProjectZipCode(),
      b.getProjectLinkOrigin(),
      b.getValidUntil(),
      b.getCommercialTerms(),
      b.getNotes(),
      b.getTaxPercent(),
      b.getMarginPercent(),
      b.getStatus(),
      b.getFinalizedAt(),
      b.getApprovedAt(),
      b.getRejectedAt(),
      cost,
      sale,
      ss);
  }

  private ResponseStatusException error(HttpStatus s,
    String m) {
    return new ResponseStatusException(s,
      m);
  }
}
