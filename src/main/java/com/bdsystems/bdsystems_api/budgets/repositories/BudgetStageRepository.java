package com.bdsystems.bdsystems_api.budgets.repositories;

import com.bdsystems.bdsystems_api.budgets.domains.entities.BudgetStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BudgetStageRepository extends JpaRepository<BudgetStage, UUID> {
  Optional<BudgetStage> findByIdAndBudgetIdAndBudgetCompanyId(UUID id,
    UUID budgetId,
    UUID companyId);
}
