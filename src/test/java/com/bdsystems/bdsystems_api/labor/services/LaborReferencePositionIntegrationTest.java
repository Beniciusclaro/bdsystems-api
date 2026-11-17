package com.bdsystems.bdsystems_api.labor.services;

import com.bdsystems.bdsystems_api.auth.domains.entities.*;
import com.bdsystems.bdsystems_api.auth.repositories.*;
import com.bdsystems.bdsystems_api.employee.domains.entities.*;
import com.bdsystems.bdsystems_api.employee.repositories.EmployeeRepository;
import com.bdsystems.bdsystems_api.labor.domains.dtos.LaborDtos.RoleRequest;
import com.bdsystems.bdsystems_api.labor.domains.entities.*;
import com.bdsystems.bdsystems_api.labor.repositories.LaborRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LaborReferencePositionIntegrationTest {
  @Autowired
  private AddressRepository addressRepository;
  @Autowired
  private CompanyRepository companyRepository;
  @Autowired
  private EmployeeRepository employeeRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private LaborRoleRepository laborRoleRepository;
  @Autowired
  private LaborService laborService;

  @Test
  void groupsCaseInsensitivelyExcludesEveryAssociatedUserAndIsolatesTenant() {
    Company company = company("Primary");
    Company foreignCompany = company("Foreign");
    employee(company,
      " Carpenter ",
      "primary-carpenter@example.com");
    employee(company,
      "carpenter",
      "second-carpenter@example.com");
    employee(company,
      "Electrician",
      "electrician@example.com");
    Employee linkedEmployee = employee(company,
      "Plumber",
      "linked-plumber@example.com");
    userRepository.save(User.invited("Linked user",
      linkedEmployee.getEmail(),
      "unusable",
      Role.USER,
      company,
      linkedEmployee));
    employee(foreignCompany,
      "Surveyor",
      "foreign-surveyor@example.com");

    assertThat(laborService.referencePositions(company.getId())).containsExactly("Carpenter",
      "Electrician");
    assertThat(employeeRepository.findEligibleForAverage(company.getId(),
      EmployeeStatus.ACTIVE,
      "  CARPENTER ")).hasSize(2);
  }

  @Test
  void calculatesAllPaymentMethodsRoundsAndRejectsMissingAverageCost() {
    Company company = company("Cost calculation");
    employee(company,
      "Estimator",
      "monthly@example.com",
      PaymentMethod.MONTHLY,
      "1600");
    employee(company,
      " estimator ",
      "weekly@example.com",
      PaymentMethod.WEEKLY,
      "480");
    employee(company,
      "ESTIMATOR",
      "daily@example.com",
      PaymentMethod.DAILY,
      "80");
    employee(company,
      "Estimator",
      "hourly@example.com",
      PaymentMethod.HOURLY,
      "14.0002");
    LaborRole averageRole = role(company,
      LaborCostingMode.EMPLOYEE_AVERAGE,
      null,
      "Estimator");
    LaborRole missingRole = role(company,
      LaborCostingMode.EMPLOYEE_AVERAGE,
      null,
      "Plumber");
    LaborRole legacyRole = role(company,
      LaborCostingMode.EMPLOYEE_AVERAGE,
      null,
      null);
    LaborRole manualRole = role(company,
      LaborCostingMode.MANUAL,
      new BigDecimal("27.1234"),
      null);

    assertThat(laborService.hourlyCost(averageRole,
      company.getId())).isEqualByComparingTo("12.0001");
    assertThat(laborService.hourlyCost(manualRole,
      company.getId())).isEqualByComparingTo("27.1234");
    assertThatThrownBy(() -> laborService.hourlyCost(missingRole,
      company.getId())).hasMessageContaining("422");
    assertThatThrownBy(() -> laborService.hourlyCost(legacyRole,
      company.getId())).hasMessageContaining("422");
  }

  @Test
  void validatesMutuallyExclusiveCostSources() {
    Company company = company("Validation");
    RoleRequest invalidManual = new RoleRequest("MANUAL",
      "Manual",
      null,
      LaborCostingMode.MANUAL,
      BigDecimal.TEN,
      "Carpenter",
      true);
    RoleRequest invalidAverage = new RoleRequest("AVERAGE",
      "Average",
      null,
      LaborCostingMode.EMPLOYEE_AVERAGE,
      BigDecimal.TEN,
      null,
      true);

    assertThatThrownBy(() -> laborService.create(invalidManual,
      company.getId())).hasMessageContaining("400");
    assertThatThrownBy(() -> laborService.create(invalidAverage,
      company.getId())).hasMessageContaining("400");
  }

  private Company company(String name) {
    String suffix = UUID.randomUUID().toString();
    Address address = addressRepository.save(new Address("Street " + suffix,
      "Lisbon",
      "Lisbon",
      "1000-001"));
    return companyRepository.save(new Company(name + suffix,
      address));
  }

  private Employee employee(Company company,
    String position,
    String email) {
    return employee(company,
      position,
      email,
      PaymentMethod.HOURLY,
      "10");
  }

  private Employee employee(Company company,
    String position,
    String email,
    PaymentMethod paymentMethod,
    String paymentAmount) {
    Employee employee = new Employee(email,
      email,
      null,
      null,
      null,
      null,
      company,
      UUID.randomUUID().toString(),
      position,
      null,
      null,
      null,
      EmployeeStatus.ACTIVE,
      paymentMethod,
      new BigDecimal(paymentAmount));
    return employeeRepository.save(employee);
  }

  private LaborRole role(Company company,
    LaborCostingMode costingMode,
    BigDecimal manualHourlyCost,
    String referencePosition) {
    LaborRole laborRole = new LaborRole(company);
    laborRole.update(UUID.randomUUID().toString(),
      "Role",
      null,
      costingMode,
      manualHourlyCost,
      referencePosition,
      true);
    return laborRoleRepository.save(laborRole);
  }
}
