package com.bdsystems.builder_api.auth.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    private static final String PASSWORD = "correct-horse-battery-staple";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registersLogsInAndProtectsRoutesByTenant() throws Exception {
        register("Acme", "owner@example.com");

        mockMvc.perform(get("/"))
                .andExpect(status().isUnauthorized());

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"OWNER@example.com","password":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("token").asText();
        mockMvc.perform(get("/").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void register(String companyName, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyName":"%s","email":"%s","password":"%s"}
                                """.formatted(companyName, email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertTrue(response.path("token").isTextual());
    }
}
