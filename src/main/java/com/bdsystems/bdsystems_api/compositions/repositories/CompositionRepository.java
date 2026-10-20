package com.bdsystems.bdsystems_api.compositions.repositories;

import com.bdsystems.bdsystems_api.compositions.domains.entities.Composition;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

import java.util.*;

public interface CompositionRepository extends JpaRepository<Composition, UUID> {
  @EntityGraph(attributePaths = {"unitOfMeasure", "components", "components.material", "components.supplierMaterial", "components.laborRole", "components.toolRental", "components.toolRental.tool"})
  Optional<Composition> findDetailedByIdAndCompanyId(UUID id,
    UUID companyId);

  boolean existsByCompanyIdAndCodeAndIdNot(UUID companyId,
    String code,
    UUID id);

  @Query("select c from Composition c where c.company.id=:companyId and (:active is null or c.active=:active) and lower(c.name) like lower(concat('%',:search,'%'))")
  Page<Composition> search(UUID companyId,
    Boolean active,
    String search,
    Pageable p);
}
