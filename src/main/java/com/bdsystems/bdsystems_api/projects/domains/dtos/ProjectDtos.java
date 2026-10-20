package com.bdsystems.bdsystems_api.projects.domains.dtos;

import com.bdsystems.bdsystems_api.projects.domains.entities.ProjectStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class ProjectDtos {
  private ProjectDtos() {
  }

  public record Request(
    @NotNull UUID clientId,
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 180) String name,
    String description,
    @Size(max = 180) String street,
    @Size(max = 100) String city,
    @Size(max = 100) String state,
    @Size(max = 20) String zipCode,
    LocalDate plannedStartDate,
    LocalDate plannedEndDate,
    ProjectStatus status,
    Boolean active
  ) {
  }

  public record Response(
    UUID id,
    UUID clientId,
    String clientName,
    String code,
    String name,
    String description,
    String street,
    String city,
    String state,
    String zipCode,
    LocalDate plannedStartDate,
    LocalDate plannedEndDate,
    ProjectStatus status,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
  ) {
  }
}
