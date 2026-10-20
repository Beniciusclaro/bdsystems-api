package com.bdsystems.bdsystems_api.materials.domains.dtos;

import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull Boolean active) {
}
