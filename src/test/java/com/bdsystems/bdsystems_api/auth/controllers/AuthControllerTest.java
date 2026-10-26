package com.bdsystems.bdsystems_api.auth.controllers;

import com.bdsystems.bdsystems_api.auth.domains.dtos.UserRequest;
import com.bdsystems.bdsystems_api.auth.domains.entities.Role;
import com.bdsystems.bdsystems_api.auth.repositories.AddressRepository;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.auth.repositories.UserRepository;
import com.bdsystems.bdsystems_api.employee.domains.entities.EmployeeStatus;
import com.bdsystems.bdsystems_api.employee.repositories.EmployeeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
  private static final String PASSWORD = "correct-horse-battery-staple";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CompanyRepository companyRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EmployeeRepository employeeRepository;

  @Autowired
  private AddressRepository addressRepository;

  @Autowired
  private JwtDecoder jwtDecoder;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void registersLogsInAndProtectsRoutesByTenant() throws Exception {
    String token = register("Auth Flow Acme",
      "owner@example.com");

    var user = userRepository.findByEmail("owner@example.com").orElseThrow();
    var employee = employeeRepository.findByCompanyIdAndEmail(
      user.getCompany().getId(),
      user.getEmail()
    ).orElseThrow();
    assertEquals(Role.ADMIN,
      user.getRole());
    assertEquals(employee.getId(),
      user.getEmployee().getId());
    assertEquals(EmployeeStatus.ACTIVE,
      employee.getStatus());
    assertNull(user.getAddress());
    assertNull(employee.getAddress());
    assertNull(employee.getPosition());
    assertEquals(user.getCompany().getId().toString(),
      jwtDecoder.decode(token).getClaimAsString("tenant_id"));
    assertEquals("ADMIN",
      jwtDecoder.decode(token).getClaimAsString("role"));
    assertEquals(employee.getId().toString(),
      jwtDecoder.decode(token).getClaimAsString("employee_id"));

    mockMvc.perform(post("/api/auth/logout"))
      .andExpect(status().isUnauthorized());

    MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"email":"owner@example.com","password":"%s"}
          """.formatted(PASSWORD)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.token").isNotEmpty())
      .andReturn();

    String loginToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("token").asText();
    mockMvc.perform(post("/api/auth/logout").header("Authorization",
        "Bearer " + loginToken))
      .andExpect(status().isNoContent());
  }

  @Test
  void rejectsDuplicateCompanyOrLoginEmailWithoutPartialRegistration() throws Exception {
    register("Registration Conflict Company",
      "registration.conflict@example.com");
    long companyCount = companyRepository.count();
    long employeeCount = employeeRepository.count();
    long userCount = userRepository.count();
    long addressCount = addressRepository.count();

    registerExpectingConflict("Registration Conflict Company",
      "other.owner@example.com");
    registerExpectingConflict("Another Registration Company",
      "registration.conflict@example.com");

    assertEquals(companyCount,
      companyRepository.count());
    assertEquals(employeeCount,
      employeeRepository.count());
    assertEquals(userCount,
      userRepository.count());
    assertEquals(addressCount,
      addressRepository.count());
  }

  @Test
  void listsIncompleteEmployeeButExcludesItFromLaborAndProtectsLinkedUser() throws Exception {
    String token = register("Incomplete Employee Company",
      "incomplete.owner@example.com");
    var user = userRepository.findByEmail("incomplete.owner@example.com").orElseThrow();
    var employee = user.getEmployee();

    mockMvc.perform(get("/api/employees")
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.page.totalElements").value(1))
      .andExpect(jsonPath("$.content[0].id").value(employee.getId().toString()))
      .andExpect(jsonPath("$.content[0].address").doesNotExist())
      .andExpect(jsonPath("$.content[0].position").doesNotExist());

    mockMvc.perform(get("/api/labor-cost-profiles")
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.page.totalElements").value(0));

    mockMvc.perform(delete("/api/employees/{id}",
        employee.getId())
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isConflict());

    mockMvc.perform(put("/api/employees/{id}",
        employee.getId())
        .header("Authorization",
          "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "fullName": "Updated Linked Owner",
            "email": "updated.linked.owner@example.com",
            "phoneNumber": "+351900000009",
            "address": {
              "street": "Personal Street",
              "city": "Lisbon",
              "state": "Lisbon",
              "zipCode": "1000-009"
            },
            "employeeNumber": "OWNER-001",
            "position": "Administrator",
            "department": "Management",
            "admissionDate": "2026-01-01",
            "status": "ACTIVE"
          }
          """))
      .andExpect(status().isOk());

    var updatedUser = userRepository.findByEmail("updated.linked.owner@example.com").orElseThrow();
    assertEquals("Updated Linked Owner",
      updatedUser.getFullName());
    assertEquals(employee.getId(),
      updatedUser.getEmployee().getId());

    register("Second Linked Company",
      "second.linked.owner@example.com");
    mockMvc.perform(put("/api/employees/{id}",
        employee.getId())
        .header("Authorization",
          "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "fullName": "Conflicting Linked Owner",
            "email": "second.linked.owner@example.com",
            "phoneNumber": "+351900000009",
            "address": {
              "street": "Personal Street",
              "city": "Lisbon",
              "state": "Lisbon",
              "zipCode": "1000-009"
            },
            "employeeNumber": "OWNER-001",
            "position": "Administrator",
            "department": "Management",
            "admissionDate": "2026-01-01",
            "status": "ACTIVE"
          }
          """))
      .andExpect(status().isConflict());

    mockMvc.perform(put("/api/employees/{id}",
        employee.getId())
        .header("Authorization",
          "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "fullName": "Updated Linked Owner",
            "email": "updated.linked.owner@example.com",
            "phoneNumber": "+351900000009",
            "address": {
              "street": "Personal Street",
              "city": "Lisbon",
              "state": "Lisbon",
              "zipCode": "1000-009"
            },
            "employeeNumber": "OWNER-001",
            "position": "Administrator",
            "department": "Management",
            "admissionDate": "2026-01-01",
            "status": "INACTIVE"
          }
          """))
      .andExpect(status().isConflict());

    var unchangedEmployee = employeeRepository.findById(employee.getId()).orElseThrow();
    assertEquals("updated.linked.owner@example.com",
      unchangedEmployee.getEmail());
    assertEquals(EmployeeStatus.ACTIVE,
      unchangedEmployee.getStatus());
  }

  @Test
  void disablesLegacyPublicRegistration() throws Exception {
    mockMvc.perform(post("/user/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
      .andExpect(status().isUnauthorized());
  }

  private String register(String companyName,
    String email) throws Exception {
    UserRequest request = new UserRequest(
      "Acme Owner",
      email,
      PASSWORD,
      new UserRequest.CompanyRequest(
        companyName,
        new UserRequest.AddressRequest("Main Street",
          "Lisbon",
          "Lisbon",
          "1000-001")
      )
    );

    MvcResult result = mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.token").isNotEmpty())
      .andReturn();

    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
    org.junit.jupiter.api.Assertions.assertTrue(response.path("token").isTextual());
    return response.path("token").asText();
  }

  private void registerExpectingConflict(String companyName,
    String email) throws Exception {
    UserRequest request = new UserRequest(
      "Conflicting Owner",
      email,
      PASSWORD,
      new UserRequest.CompanyRequest(
        companyName,
        new UserRequest.AddressRequest("Conflict Street",
          "Lisbon",
          "Lisbon",
          "1000-099")
      )
    );

    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isConflict());
  }
}
