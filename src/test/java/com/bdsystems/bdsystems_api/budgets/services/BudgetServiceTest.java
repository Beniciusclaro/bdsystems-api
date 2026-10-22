package com.bdsystems.bdsystems_api.budgets.services;

import com.bdsystems.bdsystems_api.auth.domains.entities.Address;
import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.repositories.AddressRepository;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.ApprovalRequest;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.ItemRequest;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.Request;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.Response;
import com.bdsystems.bdsystems_api.budgets.domains.dtos.BudgetDtos.StageRequest;
import com.bdsystems.bdsystems_api.budgets.domains.entities.Budget;
import com.bdsystems.bdsystems_api.budgets.domains.entities.BudgetStatus;
import com.bdsystems.bdsystems_api.budgets.domains.entities.ProjectLinkOrigin;
import com.bdsystems.bdsystems_api.budgets.repositories.BudgetRepository;
import com.bdsystems.bdsystems_api.clients.domains.entities.Client;
import com.bdsystems.bdsystems_api.clients.domains.entities.ClientType;
import com.bdsystems.bdsystems_api.clients.repositories.ClientRepository;
import com.bdsystems.bdsystems_api.compositions.domains.entities.Composition;
import com.bdsystems.bdsystems_api.compositions.repositories.CompositionRepository;
import com.bdsystems.bdsystems_api.materials.domains.entities.UnitOfMeasure;
import com.bdsystems.bdsystems_api.materials.repositories.UnitOfMeasureRepository;
import com.bdsystems.bdsystems_api.projects.domains.dtos.ProjectDtos;
import com.bdsystems.bdsystems_api.projects.domains.entities.Project;
import com.bdsystems.bdsystems_api.projects.repositories.ProjectRepository;
import com.bdsystems.bdsystems_api.projects.services.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class BudgetServiceTest {
  @Autowired
  private BudgetService budgetService;
  @Autowired
  private ProjectService projectService;
  @Autowired
  private AddressRepository addressRepository;
  @Autowired
  private CompanyRepository companyRepository;
  @Autowired
  private ClientRepository clientRepository;
  @Autowired
  private UnitOfMeasureRepository unitOfMeasureRepository;
  @Autowired
  private CompositionRepository compositionRepository;
  @Autowired
  private BudgetRepository budgetRepository;
  @Autowired
  private ProjectRepository projectRepository;

  @Test
  void createsWithoutProjectScopesClientAndRequiresServicesToFinalize() {
    Company company = createCompany();
    Client client = createClient(company);
    Response created = budgetService.create(request(client,
        "B-001"),
      company.getId());
    assertEquals(BudgetStatus.DRAFT,
      created.status());
    assertNull(created.projectId());
    assertEquals("Proposed project",
      created.proposedProjectName());

    ResponseStatusException withoutServices = assertThrows(ResponseStatusException.class,
      () -> budgetService.finalizeBudget(created.id(),
        company.getId()));
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY,
      withoutServices.getStatusCode());

    Company foreignCompany = createCompany();
    ResponseStatusException foreignClient = assertThrows(ResponseStatusException.class,
      () -> budgetService.create(request(client,
          "B-FOREIGN"),
        foreignCompany.getId()));
    assertEquals(HttpStatus.NOT_FOUND,
      foreignClient.getStatusCode());
  }

  @Test
  void finalizesApprovesOnceAndRollsBackWhenProjectCreationFails() {
    Company company = createCompany();
    Client client = createClient(company);
    Response finalized = createFinalizedBudget(company,
      client,
      "B-APPROVE");
    assertEquals(BudgetStatus.FINALIZED,
      finalized.status());
    assertThrows(ResponseStatusException.class,
      () -> budgetService.update(finalized.id(),
        request(client,
          "B-CHANGED"),
        company.getId()));

    var approved = budgetService.approveAndCreateProject(finalized.id(),
      approval("PRJ-001"),
      company.getId());
    assertEquals(BudgetStatus.APPROVED,
      approved.budget().status());
    assertEquals(ProjectLinkOrigin.GENERATED,
      approved.budget().projectLinkOrigin());
    assertEquals(client.getId(),
      approved.project().clientId());
    assertEquals("Confirmed project",
      approved.project().name());
    assertNotNull(approved.budget().approvedAt());
    long projectCount = projectRepository.count();
    assertThrows(ResponseStatusException.class,
      () -> budgetService.approveAndCreateProject(finalized.id(),
        approval("PRJ-002"),
        company.getId()));
    assertEquals(projectCount,
      projectRepository.count());

    Response rollbackBudget = createFinalizedBudget(company,
      client,
      "B-ROLLBACK");
    assertThrows(ResponseStatusException.class,
      () -> budgetService.approveAndCreateProject(rollbackBudget.id(),
        approval("PRJ-001"),
        company.getId()));
    assertEquals(BudgetStatus.FINALIZED,
      budgetService.get(rollbackBudget.id(),
        company.getId()).status());
    assertEquals(projectCount,
      projectRepository.count());
  }

  @Test
  void rejectsDuplicatesWithoutApprovalDataAndApprovesLegacyByReusingProject() {
    Company company = createCompany();
    Client client = createClient(company);
    Response finalized = createFinalizedBudget(company,
      client,
      "B-REJECT");
    Response rejected = budgetService.reject(finalized.id(),
      company.getId());
    assertEquals(BudgetStatus.REJECTED,
      rejected.status());
    assertNotNull(rejected.rejectedAt());
    assertThrows(ResponseStatusException.class,
      () -> budgetService.reject(finalized.id(),
        company.getId()));

    Response duplicate = budgetService.duplicate(finalized.id(),
      "B-REVISION",
      company.getId());
    assertEquals(BudgetStatus.DRAFT,
      duplicate.status());
    assertNull(duplicate.projectId());
    assertNull(duplicate.projectLinkOrigin());
    assertNull(duplicate.approvedAt());
    assertNull(duplicate.rejectedAt());
    assertEquals(rejected.stages().size(),
      duplicate.stages().size());

    Project legacyProject = createDirectProject(company,
      client,
      "LEGACY-001");
    Response legacyFinalized = createFinalizedBudget(company,
      client,
      "B-LEGACY");
    Budget legacyBudget = budgetRepository.findById(legacyFinalized.id()).orElseThrow();
    ReflectionTestUtils.setField(legacyBudget,
      "project",
      legacyProject);
    ReflectionTestUtils.setField(legacyBudget,
      "projectLinkOrigin",
      ProjectLinkOrigin.LEGACY);
    budgetRepository.saveAndFlush(legacyBudget);
    long projectCount = projectRepository.count();
    var legacyApproval = budgetService.approveAndCreateProject(legacyFinalized.id(),
      approval("IGNORED"),
      company.getId());
    assertEquals(legacyProject.getId(),
      legacyApproval.project().id());
    assertEquals(ProjectLinkOrigin.LEGACY,
      legacyApproval.budget().projectLinkOrigin());
    assertEquals(projectCount,
      projectRepository.count());

    Company foreignCompany = createCompany();
    assertThrows(ResponseStatusException.class,
      () -> budgetService.get(legacyFinalized.id(),
        foreignCompany.getId()));
  }

  private Response createFinalizedBudget(Company company,
    Client client,
    String number) {
    Response budget = budgetService.create(request(client,
        number),
      company.getId());
    Response withStage = budgetService.addStage(budget.id(),
      new StageRequest("Services",
        null,
        0),
      company.getId());
    Composition composition = createComposition(company);
    budgetService.addItem(budget.id(),
      withStage.stages().getFirst().id(),
      new ItemRequest(composition.getId(),
        BigDecimal.ONE,
        0),
      company.getId());
    return budgetService.finalizeBudget(budget.id(),
      company.getId());
  }

  private Composition createComposition(Company company) {
    String suffix = UUID.randomUUID().toString().substring(0,
      8);
    UnitOfMeasure unitOfMeasure = unitOfMeasureRepository.save(new UnitOfMeasure(suffix,
      "Unit " + suffix,
      "u" + suffix.substring(0,
        2)));
    Composition composition = new Composition(company);
    composition.update("CMP-" + suffix,
      "Composition",
      null,
      unitOfMeasure,
      BigDecimal.ONE,
      true);
    return compositionRepository.save(composition);
  }

  private Project createDirectProject(Company company,
    Client client,
    String code) {
    ProjectDtos.Request request = new ProjectDtos.Request(client.getId(),
      code,
      "Legacy project",
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      true);
    UUID projectId = projectService.create(request,
      company.getId()).id();
    return projectRepository.findById(projectId).orElseThrow();
  }

  private Request request(Client client,
    String number) {
    return new Request(client.getId(),
      number,
      "Budget title",
      LocalDate.now().plusDays(30),
      "Terms",
      "Notes",
      new BigDecimal("5"),
      new BigDecimal("10"),
      "Proposed project",
      "Proposed description",
      "Proposed street",
      "Lisbon",
      "Lisbon",
      "1000-001");
  }

  private ApprovalRequest approval(String code) {
    return new ApprovalRequest(code,
      "Confirmed project",
      "Confirmed description",
      "Confirmed street",
      "Porto",
      "Porto",
      "4000-001",
      LocalDate.now().plusDays(1),
      LocalDate.now().plusDays(20));
  }

  private Company createCompany() {
    String suffix = UUID.randomUUID().toString();
    Address address = addressRepository.save(new Address("Street " + suffix,
      "Lisbon",
      "Lisbon",
      "1000-001"));
    return companyRepository.save(new Company("Company " + suffix,
      address));
  }

  private Client createClient(Company company) {
    Client client = new Client(company);
    client.update(ClientType.COMPANY,
      "Client " + UUID.randomUUID(),
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      true);
    return clientRepository.save(client);
  }
}
