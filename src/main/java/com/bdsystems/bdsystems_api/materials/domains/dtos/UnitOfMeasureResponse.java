package com.bdsystems.bdsystems_api.materials.domains.dtos;

import java.util.UUID;

public record UnitOfMeasureResponse(
  UUID id,
  String code,
  String name,
  String symbol,
  boolean active) {
}
