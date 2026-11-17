ALTER TABLE labor_roles
  ADD COLUMN reference_position VARCHAR(150);

-- Existing EMPLOYEE_AVERAGE roles intentionally remain with a null reference.
-- They have no live cost until edited through the new contract.
ALTER TABLE labor_roles
  ADD CONSTRAINT ck_labor_role_cost_source CHECK (
    (costing_mode = 'MANUAL' AND reference_position IS NULL)
      OR (costing_mode = 'EMPLOYEE_AVERAGE' AND manual_hourly_cost IS NULL)
  );
