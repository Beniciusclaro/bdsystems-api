package com.bdsystems.bdsystems_api.employee.controllers;

import com.bdsystems.bdsystems_api.auth.domains.entities.Address;
import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.repositories.AddressRepository;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.employee.domains.entities.Employee;
import com.bdsystems.bdsystems_api.employee.domains.entities.EmployeeStatus;
import com.bdsystems.bdsystems_api.employee.repositories.EmployeeRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        new Address("Main Street", "Lisbon", "Lisbon", "1000-001")
    );
    Company company = companyRepository.save(new Company("Acme", companyAddress));
    Instant now = Instant.now();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(
        JwsHeader.with(MacAlgorithm.HS256).build(),
        JwtClaimsSet.builder()
            .subject("test-user")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600))
            .claim("tenant_id", company.getId().toString())
            .build()
    )).getTokenValue();

    mockMvc.perform(post("/api/employees")
            .header("Authorization", "Bearer " + token)
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
                  "status": "ACTIVE"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.fullName").value("Taylor Employee"))
        .andExpect(jsonPath("$.address.city").value("Lisbon"))
        .andExpect(jsonPath("$.companyId").value(company.getId().toString()))
        .andExpect(jsonPath("$.employeeNumber").value("EMP-001"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void findsEmployeesPageableAndOnlyForAuthenticatedCompany() throws Exception {
    Company company = createCompany("Acme");
    Company anotherCompany = createCompany("Other Company");
    createEmployee(company, "Taylor Listing", "listing.taylor@example.com", "EMP-101");
    createEmployee(company, "Jordan Listing", "listing.jordan@example.com", "EMP-102");
    createEmployee(anotherCompany, "Morgan Listing", "listing.morgan@example.com", "EMP-103");

    mockMvc.perform(get("/api/employees")
            .param("page", "0")
            .param("size", "1")
            .header("Authorization", "Bearer " + tokenFor(company)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].companyId").value(company.getId().toString()))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.size").value(1));
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
    createEmployee(company, "Existing Listing", "existing.listing@example.com", "EMP-CONFLICT");

    mockMvc.perform(post("/api/employees")
            .header("Authorization", "Bearer " + tokenFor(company))
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
        new Address(name + " Street", "Lisbon", "Lisbon", "1000-001")
    );
    return companyRepository.save(new Company(name, address));
  }

  private void createEmployee(Company company, String name, String email, String number) {
    Address address = addressRepository.save(
        new Address(name + " Street", "Lisbon", "Lisbon", "1000-002")
    );
    employeeRepository.save(new Employee(
        name,
        email,
        "+351900000000",
        number,
        LocalDate.of(1990, 1, 15),
        address,
        company,
        number,
        "Engineer",
        "Product",
        LocalDate.of(2025, 1, 10),
        null,
        EmployeeStatus.ACTIVE
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
            .claim("tenant_id", company.getId().toString())
            .build()
    )).getTokenValue();
  }
}
