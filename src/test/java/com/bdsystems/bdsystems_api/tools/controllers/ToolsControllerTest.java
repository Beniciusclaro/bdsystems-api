package com.bdsystems.bdsystems_api.tools.controllers;

import com.bdsystems.bdsystems_api.auth.domains.entities.Address;
import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.repositories.AddressRepository;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.supplyer.domains.entities.Supplier;
import com.bdsystems.bdsystems_api.supplyer.repositories.SupplierRepository;
import com.bdsystems.bdsystems_api.tools.domains.entities.Tool;
import com.bdsystems.bdsystems_api.tools.repositories.ToolRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
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

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
class ToolsControllerTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private AddressRepository addressRepository;

  @Autowired
  private CompanyRepository companyRepository;

  @Autowired
  private SupplierRepository supplierRepository;

  @Autowired
  private ToolRepository toolRepository;

  @Autowired
  private JwtEncoder jwtEncoder;

  @Test
  void createsListsFiltersAndUpdatesToolsWithinTheirCompany() throws Exception {
    Company owner = createCompany();
    Company other = createCompany();
    String ownerToken = tokenFor(owner);
    String otherToken = tokenFor(other);
    String toolId = createTool(ownerToken,
      "SHARED-CODE",
      "Owner tool");

    mockMvc.perform(get("/api/tools").header("Authorization",
        "Bearer " + ownerToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content.length()").value(1))
      .andExpect(jsonPath("$.content[0].companyId").value(owner.getId().toString()));

    mockMvc.perform(post("/api/tools")
        .header("Authorization",
          "Bearer " + ownerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(toolJson("SHARED-CODE",
          "Duplicate tool")))
      .andExpect(status().isConflict());

    mockMvc.perform(post("/api/tools")
        .header("Authorization",
          "Bearer " + otherToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(toolJson("SHARED-CODE",
          "Other tenant tool")))
      .andExpect(status().isCreated());

    mockMvc.perform(get("/api/tools/{id}",
        toolId)
        .header("Authorization",
          "Bearer " + otherToken))
      .andExpect(status().isNotFound());

    mockMvc.perform(get("/api/tools/{id}",
        UUID.randomUUID())
        .header("Authorization",
          "Bearer " + ownerToken))
      .andExpect(status().isNotFound());

    mockMvc.perform(put("/api/tools/{id}",
        toolId)
        .header("Authorization",
          "Bearer " + otherToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(toolJson("CHANGED",
          "Unauthorized update")))
      .andExpect(status().isNotFound());

    mockMvc.perform(put("/api/tools/{id}",
        toolId)
        .header("Authorization",
          "Bearer " + ownerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(toolJson("UPDATED",
          "Updated tool")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("UPDATED"));

    mockMvc.perform(patch("/api/tools/{id}/deactivate",
        toolId)
        .header("Authorization",
          "Bearer " + ownerToken))
      .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/tools")
        .param("active",
          "false")
        .header("Authorization",
          "Bearer " + ownerToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content.length()").value(1));

    mockMvc.perform(patch("/api/tools/{id}/activate",
        toolId)
        .header("Authorization",
          "Bearer " + ownerToken))
      .andExpect(status().isNoContent());
  }

  @Test
  void createsListsUpdatesAndDeletesRentalsWithTenantScopedAssociations() throws Exception {
    Company owner = createCompany();
    Company other = createCompany();
    Tool tool = createTool(owner,
      "TOOL-RENTAL");
    Supplier supplier = createSupplier(owner);
    Tool otherTool = createTool(other,
      "OTHER-TOOL");
    Supplier otherSupplier = createSupplier(other);
    String ownerToken = tokenFor(owner);
    String otherToken = tokenFor(other);

    String rentalId = createRental(ownerToken,
      tool.getId(),
      supplier.getId());
    createRental(ownerToken,
      tool.getId(),
      supplier.getId());
    createRental(otherToken,
      otherTool.getId(),
      otherSupplier.getId());

    mockMvc.perform(get("/api/tool-rentals")
        .header("Authorization",
          "Bearer " + ownerToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content.length()").value(2))
      .andExpect(jsonPath("$.content[0].companyId").value(owner.getId().toString()));

    mockMvc.perform(get("/api/tools/{toolId}/rentals",
        tool.getId())
        .header("Authorization",
          "Bearer " + ownerToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content.length()").value(2));

    mockMvc.perform(get("/api/tools/{toolId}/rentals",
        tool.getId())
        .header("Authorization",
          "Bearer " + otherToken))
      .andExpect(status().isNotFound());

    mockMvc.perform(put("/api/tool-rentals/{id}",
        rentalId)
        .header("Authorization",
          "Bearer " + ownerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(rentalJson(tool.getId(),
          supplier.getId(),
          "125.50",
          "DAY",
          "2026-02-01",
          "2026-02-04")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.price").value(125.50))
      .andExpect(jsonPath("$.rentalPeriod").value("DAY"));

    mockMvc.perform(put("/api/tool-rentals/{id}",
        rentalId)
        .header("Authorization",
          "Bearer " + otherToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(rentalJson(otherTool.getId(),
          otherSupplier.getId(),
          "1.00",
          "DAY",
          null,
          null)))
      .andExpect(status().isNotFound());

    mockMvc.perform(delete("/api/tool-rentals/{id}",
        rentalId)
        .header("Authorization",
          "Bearer " + ownerToken))
      .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/tool-rentals/{id}",
        rentalId)
        .header("Authorization",
          "Bearer " + ownerToken))
      .andExpect(status().isNotFound());
  }

  @Test
  void rejectsInvalidRentalPriceDatesAndCrossCompanyReferences() throws Exception {
    Company owner = createCompany();
    Company other = createCompany();
    Tool ownerTool = createTool(owner,
      "VALID-TOOL");
    Supplier ownerSupplier = createSupplier(owner);
    Tool otherTool = createTool(other,
      "FOREIGN-TOOL");
    Supplier otherSupplier = createSupplier(other);
    String ownerToken = tokenFor(owner);

    mockMvc.perform(post("/api/tool-rentals")
        .header("Authorization",
          "Bearer " + ownerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(rentalJson(ownerTool.getId(),
          ownerSupplier.getId(),
          "-1.00",
          "DAY",
          null,
          null)))
      .andExpect(status().isBadRequest());

    mockMvc.perform(post("/api/tool-rentals")
        .header("Authorization",
          "Bearer " + ownerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(rentalJson(ownerTool.getId(),
          ownerSupplier.getId(),
          "10.00",
          "DAY",
          "2026-02-10",
          "2026-02-09")))
      .andExpect(status().isBadRequest());

    mockMvc.perform(post("/api/tool-rentals")
        .header("Authorization",
          "Bearer " + ownerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(rentalJson(otherTool.getId(),
          ownerSupplier.getId(),
          "10.00",
          "DAY",
          null,
          null)))
      .andExpect(status().isNotFound());

    mockMvc.perform(post("/api/tool-rentals")
        .header("Authorization",
          "Bearer " + ownerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(rentalJson(ownerTool.getId(),
          otherSupplier.getId(),
          "10.00",
          "DAY",
          null,
          null)))
      .andExpect(status().isNotFound());
  }

  @Test
  void documentsToolAndToolRentalEndpointsInSwaggerSpecifications() throws Exception {
    mockMvc.perform(get("/openapi.yml"))
      .andExpect(status().isOk())
      .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/tools")))
      .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/tool-rentals")));

    mockMvc.perform(get("/tool/openapi.yml"))
      .andExpect(status().isOk())
      .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/tools/{id}/activate")));

    mockMvc.perform(get("/tool-rental/openapi.yml"))
      .andExpect(status().isOk())
      .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/tools/{toolId}/rentals")))
      .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/tool-rentals/{id}")));
  }

  private String createTool(String token,
    String code,
    String name) throws Exception {
    String response = mockMvc.perform(post("/api/tools")
        .header("Authorization",
          "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(toolJson(code,
          name)))
      .andExpect(status().isCreated())
      .andReturn()
      .getResponse()
      .getContentAsString();
    return jsonMapper.readTree(response).get("id").asText();
  }

  private Tool createTool(Company company,
    String code) {
    return toolRepository.save(new Tool(company,
      code,
      "Tool " + code));
  }

  private String createRental(String token,
    UUID toolId,
    UUID supplierId) throws Exception {
    String response = mockMvc.perform(post("/api/tool-rentals")
        .header("Authorization",
          "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(rentalJson(toolId,
          supplierId,
          "100.00",
          "WEEK",
          "2026-01-01",
          "2026-01-07")))
      .andExpect(status().isCreated())
      .andReturn()
      .getResponse()
      .getContentAsString();
    return jsonMapper.readTree(response).get("id").asText();
  }

  private Company createCompany() {
    String suffix = UUID.randomUUID().toString();
    Address address = addressRepository.save(
      new Address("Street " + suffix,
        "Lisbon",
        "Lisbon",
        "1000-001")
    );
    return companyRepository.save(new Company("Company " + suffix,
      address));
  }

  private Supplier createSupplier(Company company) {
    String suffix = UUID.randomUUID().toString();
    Address address = addressRepository.save(
      new Address("Supplier Street " + suffix,
        "Lisbon",
        "Lisbon",
        "1000-002")
    );
    return supplierRepository.save(new Supplier(
      company,
      "Supplier " + suffix,
      null,
      null,
      null,
      null,
      address
    ));
  }

  private String toolJson(String code,
    String name) {
    return """
      {"code":"%s","name":"%s"}
      """.formatted(code,
      name);
  }

  private String rentalJson(
    UUID toolId,
    UUID supplierId,
    String price,
    String period,
    String startDate,
    String endDate
  ) {
    String start = startDate == null ? "null" : "\"" + startDate + "\"";
    String end = endDate == null ? "null" : "\"" + endDate + "\"";
    return """
      {
        "toolId":"%s",
        "supplierId":"%s",
        "price":%s,
        "rentalPeriod":"%s",
        "rentalStartDate":%s,
        "rentalEndDate":%s
      }
      """.formatted(toolId,
      supplierId,
      price,
      period,
      start,
      end);
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
