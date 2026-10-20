package com.bdsystems.bdsystems_api.materials.controllers;

import com.bdsystems.bdsystems_api.materials.repositories.UnitOfMeasureRepository;
import com.bdsystems.bdsystems_api.materials.repositories.MaterialRepository;
import com.bdsystems.bdsystems_api.materials.repositories.SupplierMaterialRepository;
import com.bdsystems.bdsystems_api.auth.domains.entities.Address;
import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.repositories.AddressRepository;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.supplyer.domains.entities.Supplier;
import com.bdsystems.bdsystems_api.supplyer.repositories.SupplierRepository;
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
class MaterialsControllerTest {

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
    private UnitOfMeasureRepository units;

    @Autowired
    private MaterialRepository materials;

    @Autowired
    private SupplierMaterialRepository associations;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private javax.sql.DataSource dataSource;

    private UUID unit() {
        return units.save(new com.bdsystems.bdsystems_api.materials.domains.entities.UnitOfMeasure(
                UUID.randomUUID().toString().substring(0, 8), "Unidade", "un")).getId();
    }
    private String material(String code, UUID unit, String category, String suppliers) {
        return "{\"code\":\"%s\",\"name\":\"Cimento\",\"unitOfMeasureId\":\"%s\",\"categoryId\":%s,\"suppliers\":%s}"
                .formatted(code, unit, category, suppliers);
    }
    private String supplier(UUID id, String price) {
        return "{\"supplierId\":\"%s\",\"supplierCode\":\"CIM\",\"referencePrice\":%s}".formatted(id, price);
    }
    private org.springframework.test.web.servlet.ResultActions send(String url, String token, String json) throws Exception {
        return mockMvc.perform(post(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
    private String id(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void exposesOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/openapi.yml")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/materials")));
        mockMvc.perform(get("/material/openapi.yml")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("SupplierMaterialRequest")));
    }

    @Test
    void createsWithoutSuppliersRejectsDuplicatesAndAllowsSameCodeInAnotherCompany() throws Exception {
        Company owner = createCompany(); Company other = createCompany();
        String token = tokenFor(owner); UUID unit = unit();
        String json = material("MAT-001", unit, "null", "null");
        String id = id(send("/api/materials", token, json).andExpect(status().isCreated()));
        send("/api/materials", token, json).andExpect(status().isConflict());
        send("/api/materials", tokenFor(other), json).andExpect(status().isCreated());
        mockMvc.perform(get("/api/materials/{id}", id).header("Authorization", "Bearer " + tokenFor(other)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/materials").param("name", "Cim").param("code", "001")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
        mockMvc.perform(put("/api/materials/{id}", id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(material("MAT-002", unit, "null", "[]")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("MAT-002"));
        mockMvc.perform(patch("/api/materials/{id}/status", id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/materials/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void managesMultipleSuppliersPreservesOmittedAssociationsAndScopesEveryOperation() throws Exception {
        Company owner = createCompany(); String token = tokenFor(owner); String foreign = tokenFor(createCompany());
        Supplier a = createSupplier(owner); Supplier b = createSupplier(owner); UUID unit = unit();
        String list = "[" + supplier(a.getId(), "5.90") + "," + supplier(b.getId(), "6.20") + "]";
        String id = id(send("/api/materials", token, material("MULTI", unit, "null", list)).andExpect(status().isCreated()));
        String url = "/api/materials/" + id + "/suppliers";
        var response = mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2));
        String association = jsonMapper.readTree(response.andReturn().getResponse().getContentAsString()).get("content").get(0).get("id").asText();
        String supplierId = jsonMapper.readTree(response.andReturn().getResponse().getContentAsString()).get("content").get(0).get("supplierId").asText();
        send(url, token, supplier(a.getId(), "1.00")).andExpect(status().isConflict());
        mockMvc.perform(put("/api/materials/" + id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(material("MULTI", unit, "null", "[]"))).andExpect(status().isOk());
        mockMvc.perform(get(url).header("Authorization", "Bearer " + token)).andExpect(jsonPath("$.content.length()").value(2));
        mockMvc.perform(put(url + "/" + association).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(supplier(UUID.fromString(supplierId), "9.99")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.referencePrice").value(9.99));
        mockMvc.perform(get(url).header("Authorization", "Bearer " + foreign)).andExpect(status().isNotFound());
        send(url, foreign, supplier(a.getId(), "1")).andExpect(status().isNotFound());
        mockMvc.perform(delete(url + "/" + association).header("Authorization", "Bearer " + foreign)).andExpect(status().isNotFound());
        mockMvc.perform(put(url + "/" + association).header("Authorization", "Bearer " + foreign)
                .contentType(MediaType.APPLICATION_JSON).content(supplier(UUID.fromString(supplierId), "1"))).andExpect(status().isNotFound());
        mockMvc.perform(delete(url + "/" + association).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        mockMvc.perform(get(url).header("Authorization", "Bearer " + token)).andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void rollsBackMaterialAndEarlierAssociationsWhenLaterSupplierIsForeignOrDuplicated() throws Exception {
        Company owner = createCompany(); String token = tokenFor(owner); UUID unit = unit();
        Supplier own = createSupplier(owner); Supplier other = createSupplier(createCompany());
        long materialCount = materials.count(); long associationCount = associations.count();
        send("/api/materials", token, material("ROLLBACK", unit, "null",
                "[" + supplier(own.getId(), "1") + "," + supplier(other.getId(), "2") + "]"))
                .andExpect(status().isNotFound());
        org.junit.jupiter.api.Assertions.assertEquals(materialCount, materials.count());
        org.junit.jupiter.api.Assertions.assertEquals(associationCount, associations.count());
        send("/api/materials", token, material("DUP", unit, "null",
                "[" + supplier(own.getId(), "1") + "," + supplier(own.getId(), "2") + "]"))
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertEquals(materialCount, materials.count());
        org.junit.jupiter.api.Assertions.assertEquals(associationCount, associations.count());
    }

    @Test
    void validatesCategoryOwnershipCategoryLifecycleAndRequestValues() throws Exception {
        Company owner = createCompany(); String token = tokenFor(owner); UUID unit = unit();
        String category = id(send("/api/material-categories", token, "{\"name\":\"Estrutura\"}").andExpect(status().isCreated()));
        send("/api/material-categories", token, "{\"name\":\"Estrutura\"}").andExpect(status().isConflict());
        String otherToken = tokenFor(createCompany());
        send("/api/materials", otherToken, material("FOREIGN", unit, "\"" + category + "\"", "null")).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/material-categories/" + category).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Acabamento\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Acabamento"));
        mockMvc.perform(patch("/api/material-categories/" + category + "/status").header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/material-categories/" + category + "/status").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/material-categories").param("active", "false").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(1));
        send("/api/materials", token, material("BAD", UUID.randomUUID(), "null", "null")).andExpect(status().isNotFound());
        send("/api/materials", token, "{}").andExpect(status().isBadRequest());
        for (String price : new String[]{"-1", "1.001", "10000000000"}) {
            send("/api/materials", token, material("PRICE", unit, "null", "[" + supplier(createSupplier(owner).getId(), price) + "]"))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(get("/api/units-of-measure").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/materials")).andExpect(status().isUnauthorized());
    }
    @Test
    void rejectsCrossTenantWritesAndAssociationIdsFromAnotherMaterial() throws Exception {
        Company owner = createCompany(); String token = tokenFor(owner); String foreign = tokenFor(createCompany());
        UUID unit = unit(); Supplier supplier = createSupplier(owner);
        String first = id(send("/api/materials", token, material("FIRST", unit, "null", "null")).andExpect(status().isCreated()));
        String second = id(send("/api/materials", token, material("SECOND", unit, "null", "null")).andExpect(status().isCreated()));
        String association = id(send("/api/materials/" + first + "/suppliers", token, supplier(supplier.getId(), "1"))
            .andExpect(status().isCreated()));
        mockMvc.perform(put("/api/materials/" + first).header("Authorization", "Bearer " + foreign)
            .contentType(MediaType.APPLICATION_JSON).content(material("CHANGED", unit, "null", "null")))
            .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/materials/" + first + "/status").header("Authorization", "Bearer " + foreign)
            .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
            .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/materials/" + second + "/suppliers/" + association).header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/materials/" + first).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(material("FIRST", unit, "null", "[" + supplier(supplier.getId(), "2.50") + "]")))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/materials/" + first + "/suppliers").header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.content[0].referencePrice").value(2.50));
        String invalidTenant = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
            JwtClaimsSet.builder().subject("test-user").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
                .claim("tenant_id", "invalid").build())).getTokenValue();
        mockMvc.perform(get("/api/materials").header("Authorization", "Bearer " + invalidTenant)).andExpect(status().isForbidden());
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "spring.profiles.active", matches = "materials-postgres")
    void validatesSeedIdempotenceAndDatabaseConstraints() throws Exception {
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        org.junit.jupiter.api.Assertions.assertEquals(10, jdbc.queryForObject(
            "select count(*) from units_of_measure where id::text like 'f0000000-%'", Integer.class));
        String seed = new String(new org.springframework.core.io.ClassPathResource(
            "db/migration/V9__seed_units_of_measure.sql").getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        jdbc.execute(seed);
        org.junit.jupiter.api.Assertions.assertEquals(10, jdbc.queryForObject(
            "select count(*) from units_of_measure where id::text like 'f0000000-%'", Integer.class));
        Company owner = createCompany(); String token = tokenFor(owner); UUID unit = unit();
        Supplier supplier = createSupplier(owner);
        String materialId = id(send("/api/materials", token, material("DB", unit, "null", "null")).andExpect(status().isCreated()));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbc.update("insert into materials(id, company_id, code, name, unit_of_measure_id) values (?, ?, 'DB', 'Duplicate', ?)",
                UUID.randomUUID(), owner.getId(), unit));
        String insert = "insert into supplier_materials(id, material_id, supplier_id, reference_price) values (?, ?, ?, ?)";
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbc.update(insert, UUID.randomUUID(), UUID.fromString(materialId), supplier.getId(), -1));
        jdbc.update(insert, UUID.randomUUID(), UUID.fromString(materialId), supplier.getId(), 1);
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbc.update(insert, UUID.randomUUID(), UUID.fromString(materialId), supplier.getId(), 1));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbc.update(insert, UUID.randomUUID(), UUID.randomUUID(), supplier.getId(), 1));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbc.update("insert into units_of_measure(id, code, name, symbol) values (?, 'UN', 'Duplicate', 'un')", UUID.randomUUID()));
        send("/api/material-categories", token, "{\"name\":\"DB category\"}").andExpect(status().isCreated());
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbc.update("insert into material_categories(id, company_id, name) values (?, ?, 'DB category')", UUID.randomUUID(), owner.getId()));
    }

    private Company createCompany() {
        String suffix = UUID.randomUUID().toString();
        Address address = addressRepository.save(
                new Address("Street " + suffix, "Lisbon", "Lisbon", "1000-001")
        );
        return companyRepository.save(new Company("Company " + suffix, address));
    }

    private Supplier createSupplier(Company company) {
        String suffix = UUID.randomUUID().toString();
        Address address = addressRepository.save(
                new Address("Supplier Street " + suffix, "Lisbon", "Lisbon", "1000-002")
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
