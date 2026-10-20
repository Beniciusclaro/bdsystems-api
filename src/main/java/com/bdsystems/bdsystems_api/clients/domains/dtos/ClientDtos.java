package com.bdsystems.bdsystems_api.clients.domains.dtos;

import com.bdsystems.bdsystems_api.clients.domains.entities.ClientType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class ClientDtos {
  private ClientDtos() {
  }

  public record Request(
    @NotNull ClientType type,
    @NotBlank @Size(max = 180) String name,
    @Size(max = 32) String document,
    @Email @Size(max = 180) String email,
    @Size(max = 40) String phone,
    @Size(max = 180) String street,
    @Size(max = 100) String city,
    @Size(max = 100) String state,
    @Size(max = 20) String zipCode,
    Boolean active
  ) {
  }

  public record Response(
    UUID id,
    ClientType type,
    String name,
    String document,
    String email,
    String phone,
    String street,
    String city,
    String state,
    String zipCode,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
  ) {
  }
}
