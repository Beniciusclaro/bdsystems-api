package com.bdsystems.bdsystems_api.planning;

import com.bdsystems.bdsystems_api.auth.domains.entities.Address;
import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.repositories.AddressRepository;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.budgets.domains.entities.Budget;
import com.bdsystems.bdsystems_api.budgets.domains.entities.BudgetStatus;
import com.bdsystems.bdsystems_api.budgets.repositories.BudgetRepository;
import com.bdsystems.bdsystems_api.budgets.services.BudgetService;
import com.bdsystems.bdsystems_api.clients.domains.entities.Client;
import com.bdsystems.bdsystems_api.clients.domains.entities.ClientType;
import com.bdsystems.bdsystems_api.clients.repositories.ClientRepository;
import com.bdsystems.bdsystems_api.compositions.domains.entities.Composition;
import com.bdsystems.bdsystems_api.compositions.repositories.CompositionRepository;
import com.bdsystems.bdsystems_api.compositions.services.CompositionService;
import com.bdsystems.bdsystems_api.employee.domains.entities.Employee;
import com.bdsystems.bdsystems_api.employee.domains.entities.EmployeeStatus;
import com.bdsystems.bdsystems_api.employee.domains.entities.PaymentMethod;
import com.bdsystems.bdsystems_api.employee.repositories.EmployeeRepository;
import com.bdsystems.bdsystems_api.labor.domains.dtos.LaborDtos.ProfileListResponse;
import com.bdsystems.bdsystems_api.labor.domains.entities.EmployeeLaborCost;
import com.bdsystems.bdsystems_api.labor.domains.entities.LaborCostingMode;
import com.bdsystems.bdsystems_api.labor.domains.entities.LaborRole;
import com.bdsystems.bdsystems_api.labor.repositories.EmployeeLaborCostRepository;
import com.bdsystems.bdsystems_api.labor.repositories.LaborRoleRepository;
import com.bdsystems.bdsystems_api.labor.services.LaborService;
import com.bdsystems.bdsystems_api.materials.domains.entities.UnitOfMeasure;
import com.bdsystems.bdsystems_api.materials.repositories.UnitOfMeasureRepository;
import com.bdsystems.bdsystems_api.projects.domains.entities.Project;
import com.bdsystems.bdsystems_api.projects.domains.entities.ProjectStatus;
import com.bdsystems.bdsystems_api.projects.repositories.ProjectRepository;
import com.bdsystems.bdsystems_api.projects.services.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanningFiltersIntegrationTest {
  private static final PageRequest PAGEABLE = PageRequest.of(0,
    20);

  @Autowired
  private AddressRepository addressRepository;
  @Autowired
  private CompanyRepository companyRepository;
  @Autowired
  private ClientRepository clientRepository;
  @Autowired
  private ProjectRepository projectRepository;
  @Autowired
  private ProjectService projectService;
  @Autowired
  private BudgetRepository budgetRepository;
  @Autowired
  private BudgetService budgetService;
  @Autowired
  private UnitOfMeasureRepository unitOfMeasureRepository;
  @Autowired
  private CompositionRepository compositionRepository;
  @Autowired
  private CompositionService compositionService;
  @Autowired
  private LaborRoleRepository laborRoleRepository;
  @Autowired
  private EmployeeRepository employeeRepository;
  @Autowired
  private EmployeeLaborCostRepository employeeLaborCostRepository;
  @Autowired
  private LaborService laborService;
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private JwtEncoder jwtEncoder;

  @Test
  void filtersBudgetsByEverySearchFieldStatusAndTenant() {
    Company company = createCompany("Budget filters");
    Client client = createClient(company,
      "Alvorada Customer");
    Project project = createProject(company,
      client,
      "WORK-77",
      "Harbour Renovation",
      ProjectStatus.ACTIVE);
    Budget budget = createBudget(company,
      client,
      project,
      "QUOTE-908",
      "Premium Estimate",
      "Future Library",
      BudgetStatus.APPROVED);
    Company foreignCompany = createCompany("Foreign budgets");
    createBudget(foreignCompany,
      createClient(foreignCompany,
        "Alvorada Customer"),
      null,
      "QUOTE-FOREIGN",
      "Premium Estimate",
      "Future Library",
      BudgetStatus.APPROVED);

    assertBudgetSearch(company,
      "quote-908",
      budget);
    assertBudgetSearch(company,
      "PREMIUM",
      budget);
    assertBudgetSearch(company,
      "alvorada",
      budget);
    assertBudgetSearch(company,
      "HARBOUR",
      budget);
    assertBudgetSearch(company,
      "library",
      budget);
    assertThat(budgetService.list(company.getId(),
      "",
      BudgetStatus.APPROVED,
      PAGEABLE).getContent()).extracting(response -> response.id()).containsExactly(budget.getId());
    assertThat(budgetService.list(company.getId(),
      "premium",
      BudgetStatus.DRAFT,
      PAGEABLE)).isEmpty();
    assertThat(budgetService.list(foreignCompany.getId(),
      "quote-908",
      null,
      PAGEABLE)).isEmpty();
  }

  @Test
  void filtersProjectsAndCompositionsByAllSupportedFields() {
    Company company = createCompany("Project filters");
    Client client = createClient(company,
      "Northwind Holdings");
    Project project = createProject(company,
      client,
      "SITE-X9",
      "Ocean Tower",
      ProjectStatus.ON_HOLD);
    Project inactiveProject = createProject(company,
      client,
      "SITE-Z1",
      "Inactive Tower",
      ProjectStatus.ON_HOLD);
    ReflectionTestUtils.setField(inactiveProject,
      "active",
      false);
    projectRepository.save(inactiveProject);
    Company foreignCompany = createCompany("Foreign projects");
    createProject(foreignCompany,
      createClient(foreignCompany,
        "Northwind Holdings"),
      "SITE-FOREIGN",
      "Ocean Tower",
      ProjectStatus.ON_HOLD);

    assertProjectSearch(company,
      "site-x9",
      project);
    assertProjectSearch(company,
      "OCEAN",
      project);
    assertThat(projectService.list(company.getId(),
      null,
      null,
      "northwind",
      null,
      PAGEABLE).getContent()).extracting(response -> response.id()).containsExactlyInAnyOrder(project.getId(),
      inactiveProject.getId());
    assertThat(projectService.list(company.getId(),
      true,
      client.getId(),
      "",
      ProjectStatus.ON_HOLD,
      PAGEABLE).getContent()).extracting(response -> response.id()).containsExactly(project.getId());
    assertThat(projectService.list(foreignCompany.getId(),
      null,
      null,
      "site-x9",
      null,
      PAGEABLE)).isEmpty();

    Composition composition = createComposition(company,
      "CMP-ALPHA",
      "Concrete Foundation",
      true);
    createComposition(company,
      "CMP-INACTIVE",
      "Dormant Structure",
      false);
    createComposition(foreignCompany,
      "CMP-FOREIGN",
      "Concrete Foundation",
      true);
    assertCompositionSearch(company,
      "cmp-alpha",
      composition);
    assertCompositionSearch(company,
      "FOUNDATION",
      composition);
    assertThat(compositionService.list(company.getId(),
      true,
      " ",
      PAGEABLE).getTotalElements()).isEqualTo(1);
    assertThat(compositionService.list(foreignCompany.getId(),
      null,
      "cmp-alpha",
      PAGEABLE)).isEmpty();
  }

  @Test
  void filtersLaborRolesAndListsConfiguredAndUnconfiguredActiveEmployeesOnce() {
    Company company = createCompany("Labor filters");
    LaborRole laborRole = createLaborRole(company,
      "CARP-01",
      "Master Carpenter",
      true);
    createLaborRole(company,
      "CARP-OFF",
      "Inactive Carpenter",
      false);
    Company foreignCompany = createCompany("Foreign labor");
    createLaborRole(foreignCompany,
      "CARP-FOREIGN",
      "Master Carpenter",
      true);

    assertThat(laborService.list(company.getId(),
      "carp-01",
      null,
      PAGEABLE).getContent()).extracting(response -> response.id()).containsExactly(laborRole.getId());
    assertThat(laborService.list(company.getId(),
      "MASTER",
      true,
      PAGEABLE).getContent()).extracting(response -> response.id()).containsExactly(laborRole.getId());
    assertThat(laborService.list(foreignCompany.getId(),
      "carp-01",
      null,
      PAGEABLE)).isEmpty();

    Employee configuredEmployee = createEmployee(company,
      "EMP-100",
      "Alice Builder",
      EmployeeStatus.ACTIVE);
    Employee unconfiguredEmployee = createEmployee(company,
      "EMP-200",
      "Bruno Mason",
      EmployeeStatus.ACTIVE);
    createEmployee(company,
      "EMP-300",
      "Inactive Worker",
      EmployeeStatus.INACTIVE);
    createEmployee(foreignCompany,
      "EMP-400",
      "Foreign Worker",
      EmployeeStatus.ACTIVE);
    EmployeeLaborCost profile = new EmployeeLaborCost(configuredEmployee);
    profile.update(laborRole,
      new BigDecimal("160"),
      null,
      new BigDecimal("20"),
      new BigDecimal("100"),
      new BigDecimal("60"));
    employeeLaborCostRepository.save(profile);

    assertThat(laborService.listProfiles(company.getId(),
      "",
      null,
      PAGEABLE).getContent()).hasSize(2).extracting(ProfileListResponse::employeeId).containsExactlyInAnyOrder(configuredEmployee.getId(),
      unconfiguredEmployee.getId());
    ProfileListResponse configured = laborService.listProfiles(company.getId(),
      "alice",
      true,
      PAGEABLE).getContent().getFirst();
    assertThat(configured.employeeNumber()).isEqualTo("EMP-100");
    assertThat(configured.position()).isEqualTo("Builder");
    assertThat(configured.paymentMethod()).isEqualTo(PaymentMethod.MONTHLY);
    assertThat(configured.paymentAmount()).isEqualByComparingTo("3200");
    assertThat(configured.configured()).isTrue();
    assertThat(configured.laborRoleId()).isEqualTo(laborRole.getId());
    assertThat(configured.laborRoleCode()).isEqualTo("CARP-01");
    assertThat(configured.laborRoleName()).isEqualTo("Master Carpenter");
    assertThat(configured.effectiveHourlyCost()).isEqualByComparingTo("25.0000");
    ProfileListResponse unconfigured = laborService.listProfiles(company.getId(),
      "emp-200",
      false,
      PAGEABLE).getContent().getFirst();
    assertThat(unconfigured.employeeName()).isEqualTo("Bruno Mason");
    assertThat(unconfigured.configured()).isFalse();
    assertThat(unconfigured.laborRoleId()).isNull();
    assertThat(unconfigured.effectiveHourlyCost()).isNull();
    assertThat(laborService.listProfiles(foreignCompany.getId(),
      "EMP-100",
      null,
      PAGEABLE)).isEmpty();
  }

  @Test
  void rejectsInvalidStatusEnumAtTheHttpBoundary() throws Exception {
    Company company = createCompany("Invalid enum");

    mockMvc.perform(get("/api/budgets")
        .param("status",
          "NOT_A_STATUS")
        .header("Authorization",
          "Bearer " + tokenFor(company)))
      .andExpect(status().isBadRequest());
  }

  private void assertBudgetSearch(Company company,
    String search,
    Budget expectedBudget) {
    assertThat(budgetService.list(company.getId(),
      search,
      null,
      PAGEABLE).getContent()).extracting(response -> response.id()).containsExactly(expectedBudget.getId());
  }

  private void assertProjectSearch(Company company,
    String search,
    Project expectedProject) {
    assertThat(projectService.list(company.getId(),
      null,
      null,
      search,
      null,
      PAGEABLE).getContent()).extracting(response -> response.id()).containsExactly(expectedProject.getId());
  }

  private void assertCompositionSearch(Company company,
    String search,
    Composition expectedComposition) {
    assertThat(compositionService.list(company.getId(),
      null,
      search,
      PAGEABLE).getContent()).extracting(response -> response.id()).containsExactly(expectedComposition.getId());
  }

  private Company createCompany(String name) {
    String suffix = UUID.randomUUID().toString();
    Address address = addressRepository.save(new Address("Street " + suffix,
      "Lisbon",
      "Lisbon",
      "1000-001"));
    return companyRepository.save(new Company(name + " " + suffix,
      address));
  }

  private Client createClient(Company company,
    String name) {
    Client client = new Client(company);
    client.update(ClientType.COMPANY,
      name,
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

  private Project createProject(Company company,
    Client client,
    String code,
    String name,
    ProjectStatus status) {
    Project project = new Project(company);
    project.update(client,
      code,
      name,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      status,
      true);
    return projectRepository.save(project);
  }

  private Budget createBudget(Company company,
    Client client,
    Project project,
    String number,
    String title,
    String proposedProjectName,
    BudgetStatus status) {
    Budget budget = new Budget(company);
    budget.update(client,
      number,
      title,
      null,
      null,
      null,
      BigDecimal.ZERO,
      BigDecimal.ZERO,
      proposedProjectName,
      null,
      null,
      null,
      null,
      null);
    ReflectionTestUtils.setField(budget,
      "project",
      project);
    ReflectionTestUtils.setField(budget,
      "status",
      status);
    return budgetRepository.save(budget);
  }

  private Composition createComposition(Company company,
    String code,
    String name,
    boolean active) {
    String suffix = UUID.randomUUID().toString().substring(0,
      8);
    UnitOfMeasure unitOfMeasure = unitOfMeasureRepository.save(new UnitOfMeasure("U-" + suffix,
      "Unit " + suffix,
      "u" + suffix.substring(0,
        2)));
    Composition composition = new Composition(company);
    composition.update(code,
      name,
      null,
      unitOfMeasure,
      BigDecimal.ONE,
      active);
    return compositionRepository.save(composition);
  }

  private LaborRole createLaborRole(Company company,
    String code,
    String name,
    boolean active) {
    LaborRole laborRole = new LaborRole(company);
    laborRole.update(code,
      name,
      null,
      LaborCostingMode.MANUAL,
      BigDecimal.TEN,
      active);
    return laborRoleRepository.save(laborRole);
  }

  private Employee createEmployee(Company company,
    String employeeNumber,
    String fullName,
    EmployeeStatus status) {
    String suffix = UUID.randomUUID().toString();
    Address address = addressRepository.save(new Address("Employee Street " + suffix,
      "Lisbon",
      "Lisbon",
      "1000-002"));
    return employeeRepository.save(new Employee(fullName,
      employeeNumber.toLowerCase() + "-" + suffix + "@example.com",
      null,
      null,
      null,
      address,
      company,
      employeeNumber,
      "Builder",
      "Operations",
      LocalDate.now().minusYears(1),
      null,
      status,
      PaymentMethod.MONTHLY,
      new BigDecimal("3200")));
  }

  private String tokenFor(Company company) {
    Instant now = Instant.now();
    return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
      JwtClaimsSet.builder()
        .subject("planning-filter-test")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(3600))
        .claim("tenant_id",
          company.getId().toString())
        .build())).getTokenValue();
  }
}
