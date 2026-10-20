package com.bdsystems.bdsystems_api.employee.controllers;

import com.bdsystems.bdsystems_api.auth.domains.entities.Address;
import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.repositories.AddressRepository;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.employee.domains.entities.Employee;
import com.bdsystems.bdsystems_api.employee.domains.entities.EmployeeStatus;
import com.bdsystems.bdsystems_api.employee.domains.entities.PaymentMethod;
import com.bdsystems.bdsystems_api.employee.repositories.EmployeeRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private AddressRepository addressRepository;

  @Autowired
  private CompanyRepository companyRepository;

  @Autowired
  private EmployeeRepository employeeRepository;

  @Autowired
  private JwtEncoder jwtEncoder;

  @Test
  void createsEmployeeWithPersonAndCompany() throws Exception {
    Address companyAddress = addressRepository.save(
      new Address("Main Street",
        "Lisbon",
        "Lisbon",
        "1000-001")
    );
    Company company = companyRepository.save(new Company("Acme",
      companyAddress));
    Instant now = Instant.now();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(
      JwsHeader.with(MacAlgorithm.HS256).build(),
      JwtClaimsSet.builder()
        .subject("test-user")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(3600))
        .claim("tenant_id",
          company.getId().toString())
        .build()
    )).getTokenValue();

    mockMvc.perform(post("/api/employees")
        .header("Authorization",
          "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "fullName": "Taylor Employee",
            "email": "taylor@example.com",
            "phoneNumber": "+351900000000",
            "documentNumber": "123456789",
            "birthDate": "1990-01-15",
            "address": {
              "street": "Second Street",
              "city": "Lisbon",
              "state": "Lisbon",
              "zipCode": "1000-002"
            },
            "employeeNumber": "EMP-001",
            "position": "Engineer",
            "department": "Product",
            "admissionDate": "2025-01-10",
            "status": "ACTIVE",
            "paymentMethod": "DAILY",
            "paymentAmount": 1000.00
          }
          """))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").isNotEmpty())
      .andExpect(jsonPath("$.fullName").value("Taylor Employee"))
      .andExpect(jsonPath("$.address.city").value("Lisbon"))
      .andExpect(jsonPath("$.companyId").value(company.getId().toString()))
      .andExpect(jsonPath("$.employeeNumber").value("EMP-001"))
      .andExpect(jsonPath("$.status").value("ACTIVE"))
      .andExpect(jsonPath("$.paymentMethod").value("DAILY"))
      .andExpect(jsonPath("$.paymentAmount").value(1000.00));
  }

  @Test
  void findsEmployeesPageableAndOnlyForAuthenticatedCompany() throws Exception {
    Company company = createCompany("Acme");
    Company anotherCompany = createCompany("Other Company");
    createEmployee(company,
      "Taylor Listing",
      "listing.taylor@example.com",
      "EMP-101");
    createEmployee(company,
      "Jordan Listing",
      "listing.jordan@example.com",
      "EMP-102");
    createEmployee(anotherCompany,
      "Morgan Listing",
      "listing.morgan@example.com",
      "EMP-103");

    mockMvc.perform(get("/api/employees")
        .param("page",
          "0")
        .param("size",
          "1")
        .header("Authorization",
          "Bearer " + tokenFor(company)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content.length()").value(1))
      .andExpect(jsonPath("$.content[0].companyId").value(company.getId().toString()))
      .andExpect(jsonPath("$.page.totalElements").value(2))
      .andExpect(jsonPath("$.page.totalPages").value(2))
      .andExpect(jsonPath("$.page.number").value(0))
      .andExpect(jsonPath("$.page.size").value(1));
  }

  @Test
  void filtersEmployeesByStatusPositionAndDepartmentIndividuallyOrCombined() throws Exception {
    Company company = createCompany("Employee Filter Company");
    createEmployee(
      company,
      "Alex Filter",
      "alex.filter@example.com",
      "EMP-FILTER-1",
      "Team Lead",
      "Finance",
      EmployeeStatus.ACTIVE
    );
    createEmployee(
      company,
      "Blair Filter",
      "blair.filter@example.com",
      "EMP-FILTER-2",
      "Team Lead",
      "Operations",
      EmployeeStatus.INACTIVE
    );
    createEmployee(
      company,
      "Casey Filter",
      "casey.filter@example.com",
      "EMP-FILTER-3",
      "Analyst",
      "Finance",
      EmployeeStatus.ACTIVE
    );
    String token = tokenFor(company);

    mockMvc.perform(get("/api/employees")
        .param("status",
          "ACTIVE")
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.page.totalElements").value(2));

    mockMvc.perform(get("/api/employees")
        .param("position",
          "lead")
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.page.totalElements").value(2));

    mockMvc.perform(get("/api/employees")
        .param("department",
          "FIN")
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.page.totalElements").value(2));

    mockMvc.perform(get("/api/employees")
        .param("status",
          "ACTIVE")
        .param("department",
          "FIN")
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.page.totalElements").value(2));

    mockMvc.perform(get("/api/employees")
        .param("status",
          "ACTIVE")
        .param("position",
          "lead")
        .param("department",
          "fin")
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.page.totalElements").value(1))
      .andExpect(jsonPath("$.content[0].fullName").value("Alex Filter"));
  }

  @Test
  void updatesEmployeeForAuthenticatedCompany() throws Exception {
    Company company = createCompany("Employee Update Company");
    Employee employee = createEmployee(
      company,
      "Taylor Before Update",
      "before.update@example.com",
      "EMP-UPDATE"
    );

    mockMvc.perform(put("/api/employees/{id}",
        employee.getId())
        .header("Authorization",
          "Bearer " + tokenFor(company))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "fullName": "Taylor After Update",
            "email": "after.update@example.com",
            "phoneNumber": "+351911111111",
            "documentNumber": "DOC-UPDATED",
            "birthDate": "1991-02-16",
            "address": {
              "street": "Updated Street",
              "city": "Porto",
              "state": "Porto",
              "zipCode": "4000-001"
            },
            "employeeNumber": "EMP-UPDATED",
            "position": "Senior Engineer",
            "department": "Engineering",
            "admissionDate": "2025-02-10",
            "status": "ACTIVE",
            "paymentMethod": "DAILY",
            "paymentAmount": 1500.00
          }
          """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(employee.getId().toString()))
      .andExpect(jsonPath("$.fullName").value("Taylor After Update"))
      .andExpect(jsonPath("$.email").value("after.update@example.com"))
      .andExpect(jsonPath("$.address.street").value("Updated Street"))
      .andExpect(jsonPath("$.address.city").value("Porto"))
      .andExpect(jsonPath("$.companyId").value(company.getId().toString()))
      .andExpect(jsonPath("$.employeeNumber").value("EMP-UPDATED"))
      .andExpect(jsonPath("$.paymentAmount").value(1500.00));
  }

  @Test
  void doesNotUpdateEmployeeOwnedByAnotherCompany() throws Exception {
    Company ownerCompany = createCompany("Employee Owner Company");
    Company authenticatedCompany = createCompany("Employee Other Company");
    Employee employee = createEmployee(
      ownerCompany,
      "Tenant Isolated Employee",
      "tenant.isolated@example.com",
      "EMP-TENANT"
    );

    mockMvc.perform(put("/api/employees/{id}",
        employee.getId())
        .header("Authorization",
          "Bearer " + tokenFor(authenticatedCompany))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "fullName": "Unauthorized Update",
            "email": "unauthorized.update@example.com",
            "phoneNumber": "+351922222222",
            "address": {
              "street": "Unauthorized Street",
              "city": "Lisbon",
              "state": "Lisbon",
              "zipCode": "1000-004"
            },
            "employeeNumber": "EMP-UNAUTHORIZED",
            "position": "Engineer"
          }
          """))
      .andExpect(status().isNotFound());
  }

  @Test
  void softDeletesEmployeeForAuthenticatedCompanyAndHidesItFromListing() throws Exception {
    Company company = createCompany("Employee Delete Company");
    Employee employee = createEmployee(
      company,
      "Taylor To Delete",
      "delete.taylor@example.com",
      "EMP-DELETE"
    );

    mockMvc.perform(delete("/api/employees/{id}",
        employee.getId())
        .header("Authorization",
          "Bearer " + tokenFor(company)))
      .andExpect(status().isNoContent());

    Employee deletedEmployee = employeeRepository.findById(employee.getId()).orElseThrow();
    assertTrue(deletedEmployee.isEnable());

    mockMvc.perform(get("/api/employees")
        .header("Authorization",
          "Bearer " + tokenFor(company)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content.length()").value(0))
      .andExpect(jsonPath("$.page.totalElements").value(0));
  }

  @Test
  void doesNotDeleteEmployeeOwnedByAnotherCompany() throws Exception {
    Company ownerCompany = createCompany("Delete Owner Company");
    Company authenticatedCompany = createCompany("Delete Other Company");
    Employee employee = createEmployee(
      ownerCompany,
      "Tenant Isolated Delete",
      "tenant.delete@example.com",
      "EMP-DELETE-TENANT"
    );

    mockMvc.perform(delete("/api/employees/{id}",
        employee.getId())
        .header("Authorization",
          "Bearer " + tokenFor(authenticatedCompany)))
      .andExpect(status().isNotFound());

    assertFalse(employeeRepository.findById(employee.getId()).orElseThrow().isEnable());
  }

  @Test
  void servesOpenApiDocumentsWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/auth/openapi.yml"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("openapi: 3.0.3")));

    mockMvc.perform(get("/employee/openapi.yml"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("openapi: 3.0.3")));
  }

  @Test
  void explainsConflictWhenEmployeeNumberIsAlreadyRegistered() throws Exception {
    Company company = createCompany("Conflict Test Company");
    createEmployee(company,
      "Existing Listing",
      "existing.listing@example.com",
      "EMP-CONFLICT");

    mockMvc.perform(post("/api/employees")
        .header("Authorization",
          "Bearer " + tokenFor(company))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "fullName": "Duplicate Listing",
            "email": "duplicate.listing@example.com",
            "phoneNumber": "+351900000001",
            "documentNumber": "DOC-CONFLICT",
            "birthDate": "1990-01-15",
            "address": {
              "street": "Duplicate Street",
              "city": "Lisbon",
              "state": "Lisbon",
              "zipCode": "1000-003"
            },
            "employeeNumber": "EMP-CONFLICT",
            "position": "Engineer",
            "department": "Product",
            "admissionDate": "2025-01-10",
            "status": "ACTIVE"
          }
          """))
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.message")
        .value("Employee number already registered for this company"));
  }

  private Company createCompany(String name) {
    Address address = addressRepository.save(
      new Address(name + " Street",
        "Lisbon",
        "Lisbon",
        "1000-001")
    );
    return companyRepository.save(new Company(name,
      address));
  }

  private Employee createEmployee(Company company,
    String name,
    String email,
    String number) {
    return createEmployee(
      company,
      name,
      email,
      number,
      "Engineer",
      "Product",
      EmployeeStatus.ACTIVE
    );
  }

  private Employee createEmployee(
    Company company,
    String name,
    String email,
    String number,
    String position,
    String department,
    EmployeeStatus status
  ) {
    Address address = addressRepository.save(
      new Address(name + " Street",
        "Lisbon",
        "Lisbon",
        "1000-002")
    );
    return employeeRepository.save(new Employee(
      name,
      email,
      "+351900000000",
      number,
      LocalDate.of(1990,
        1,
        15),
      address,
      company,
      number,
      position,
      department,
      LocalDate.of(2025,
        1,
        10),
      null,
      status,
      PaymentMethod.DAILY,
      new BigDecimal("1000.00")
    ));
  }

  private String tokenFor(Company company) {
    Instant now = Instant.now();
    return jwtEncoder.encode(JwtEncoderParameters.from(
      JwsHeader.with(MacAlgorithm.HS256).build(),
      JwtClaimsSet.builder()
        .subject("test-user")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(3600))
        .claim("tenant_id",
          company.getId().toString())
        .build()
    )).getTokenValue();
  }
}
