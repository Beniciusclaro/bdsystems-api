package com.bdsystems.bdsystems_api.supplyer.controllers;

import com.bdsystems.bdsystems_api.auth.domains.entities.Address;
import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.repositories.AddressRepository;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.supplyer.domains.entities.Supplier;
import com.bdsystems.bdsystems_api.supplyer.repositories.SupplierRepository;

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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SupplierControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private AddressRepository addressRepository;

  @Autowired
  private CompanyRepository companyRepository;

  @Autowired
  private SupplierRepository supplierRepository;

  @Autowired
  private JwtEncoder jwtEncoder;

  @Test
  void createsReadsUpdatesAndSoftDeletesSupplierForAuthenticatedCompany() throws Exception {
    Company company = createCompany("Supplier CRUD Company");
    String token = tokenFor(company);

    String response = mockMvc.perform(post("/api/suppliers")
        .header("Authorization",
          "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "legalName": "Acme Supplies Ltd",
            "tradeName": "Acme Supplies",
            "taxNumber": "PT123456789",
            "email": "supplier@example.com",
            "phoneNumber": "+351900000000",
            "address": {
              "street": "Second Street",
              "city": "Lisbon",
              "state": "Lisbon",
              "zipCode": "1000-002"
            }
          }
          """))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").isNotEmpty())
      .andExpect(jsonPath("$.legalName").value("Acme Supplies Ltd"))
      .andExpect(jsonPath("$.companyId").value(company.getId().toString()))
      .andExpect(jsonPath("$.active").value(true))
      .andReturn()
      .getResponse()
      .getContentAsString();
    String supplierId = com.fasterxml.jackson.databind.json.JsonMapper.builder()
      .build()
      .readTree(response)
      .get("id")
      .asText();

    mockMvc.perform(get("/api/suppliers/{id}",
        supplierId)
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.tradeName").value("Acme Supplies"))
      .andExpect(jsonPath("$.address.city").value("Lisbon"));

    mockMvc.perform(get("/api/suppliers")
        .param("page",
          "0")
        .param("size",
          "1")
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content.length()").value(1))
      .andExpect(jsonPath("$.page.totalElements").value(1));

    mockMvc.perform(put("/api/suppliers/{id}",
        supplierId)
        .header("Authorization",
          "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "legalName": "Acme Supplies Updated Ltd",
            "tradeName": "Acme Updated",
            "taxNumber": "PT987654321",
            "email": "updated@example.com",
            "phoneNumber": "+351911111111",
            "address": {
              "street": "Updated Street",
              "city": "Porto",
              "state": "Porto",
              "zipCode": "4000-001"
            }
          }
          """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.legalName").value("Acme Supplies Updated Ltd"))
      .andExpect(jsonPath("$.email").value("updated@example.com"))
      .andExpect(jsonPath("$.address.city").value("Porto"));

    mockMvc.perform(delete("/api/suppliers/{id}",
        supplierId)
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/suppliers")
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.page.totalElements").value(0));

    mockMvc.perform(get("/api/suppliers/{id}",
        supplierId)
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isNotFound());
  }

  @Test
  void doesNotExposeOrModifySupplierOwnedByAnotherCompany() throws Exception {
    Company ownerCompany = createCompany("Supplier Owner Company");
    Company authenticatedCompany = createCompany("Supplier Other Company");
    String response = mockMvc.perform(post("/api/suppliers")
        .header("Authorization",
          "Bearer " + tokenFor(ownerCompany))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "legalName": "Tenant Isolated Supplier",
            "taxNumber": "TENANT-SUPPLIER-1",
            "address": {
              "street": "Owner Street",
              "city": "Lisbon",
              "state": "Lisbon",
              "zipCode": "1000-003"
            }
          }
          """))
      .andExpect(status().isCreated())
      .andReturn()
      .getResponse()
      .getContentAsString();
    String supplierId = com.fasterxml.jackson.databind.json.JsonMapper.builder()
      .build()
      .readTree(response)
      .get("id")
      .asText();
    String otherToken = tokenFor(authenticatedCompany);

    mockMvc.perform(get("/api/suppliers/{id}",
        supplierId)
        .header("Authorization",
          "Bearer " + otherToken))
      .andExpect(status().isNotFound());

    mockMvc.perform(delete("/api/suppliers/{id}",
        supplierId)
        .header("Authorization",
          "Bearer " + otherToken))
      .andExpect(status().isNotFound());
  }

  @Test
  void filtersSuppliersByNameAndTaxNumber() throws Exception {
    Company company = createCompany("Supplier Filter Company");
    String token = tokenFor(company);
    createSupplier(company,
      "Alpha Construction Materials Ltd",
      "Alpha Store",
      "PT111222333");
    createSupplier(company,
      "Beta Industrial Supplies Ltd",
      "Central Depot",
      "PT999888777");

    mockMvc.perform(get("/api/suppliers")
        .param("name",
          "alpha")
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.page.totalElements").value(1))
      .andExpect(jsonPath("$.content[0].legalName").value("Alpha Construction Materials Ltd"));

    mockMvc.perform(get("/api/suppliers")
        .param("name",
          "central")
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.page.totalElements").value(1))
      .andExpect(jsonPath("$.content[0].tradeName").value("Central Depot"));

    mockMvc.perform(get("/api/suppliers")
        .param("taxNumber",
          "888")
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.page.totalElements").value(1))
      .andExpect(jsonPath("$.content[0].taxNumber").value("PT999888777"));

    mockMvc.perform(get("/api/suppliers")
        .param("name",
          "alpha")
        .param("taxNumber",
          "999")
        .header("Authorization",
          "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.page.totalElements").value(0));
  }

  @Test
  void servesSupplierOpenApiDocumentWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/supplier/openapi.yml"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("openapi: 3.0.3")));
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

  private void createSupplier(Company company,
    String legalName,
    String tradeName,
    String taxNumber) {
    Address address = addressRepository.save(new Address(legalName + " Street",
      "Lisbon",
      "Lisbon",
      "1000-010"));
    supplierRepository.save(new Supplier(company,
      legalName,
      tradeName,
      taxNumber,
      null,
      null,
      address));
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
