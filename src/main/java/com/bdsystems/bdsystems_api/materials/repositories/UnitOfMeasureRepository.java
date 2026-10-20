package com.bdsystems.bdsystems_api.materials.repositories;

import com.bdsystems.bdsystems_api.materials.domains.entities.UnitOfMeasure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, UUID> {
  Page<UnitOfMeasure> findByActive(boolean active,
    Pageable pageable);
}
