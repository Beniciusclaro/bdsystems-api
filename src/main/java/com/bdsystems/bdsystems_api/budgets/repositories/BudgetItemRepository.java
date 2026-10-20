package com.bdsystems.bdsystems_api.budgets.repositories;

import com.bdsystems.bdsystems_api.budgets.domains.entities.BudgetItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BudgetItemRepository extends JpaRepository<BudgetItem, UUID> {
  Optional<BudgetItem> findByIdAndStageBudgetIdAndStageBudgetCompanyId(UUID id,
    UUID budgetId,
    UUID companyId);
}
