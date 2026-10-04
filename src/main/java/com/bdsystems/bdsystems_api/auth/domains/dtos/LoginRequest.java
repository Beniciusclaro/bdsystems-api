package com.bdsystems.bdsystems_api.auth.domains.dtos;

public record LoginRequest(
    String email,
    String password
) {}
