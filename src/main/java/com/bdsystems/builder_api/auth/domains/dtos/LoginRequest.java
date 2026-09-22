package com.bdsystems.builder_api.auth.domains.dtos;

public record LoginRequest(
    String email,
    String password
) {}
