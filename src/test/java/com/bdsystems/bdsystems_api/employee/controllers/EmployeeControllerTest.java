package com.bdsystems.bdsystems_api.employee.controllers;

import com.bdsystems.bdsystems_api.auth.domains.entities.Address;
import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.repositories.AddressRepository;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import java.time.Instant;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
