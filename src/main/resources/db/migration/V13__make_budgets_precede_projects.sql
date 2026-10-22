ALTER TABLE budgets
  ALTER COLUMN project_id DROP NOT NULL,
  ADD COLUMN proposed_project_name VARCHAR(180),
  ADD COLUMN proposed_project_description TEXT,
  ADD COLUMN proposed_project_street VARCHAR(180),
  ADD COLUMN proposed_project_city VARCHAR(100),
  ADD COLUMN proposed_project_state VARCHAR(100),
  ADD COLUMN proposed_project_zip_code VARCHAR(20),
  ADD COLUMN approved_at TIMESTAMP WITH TIME ZONE,
  ADD COLUMN rejected_at TIMESTAMP WITH TIME ZONE,
  ADD COLUMN project_link_origin VARCHAR(20);

UPDATE budgets budget
SET proposed_project_name = project.name,
    proposed_project_description = project.description,
    proposed_project_street = project.street,
    proposed_project_city = project.city,
    proposed_project_state = project.state,
    proposed_project_zip_code = project.zip_code,
    project_link_origin = 'LEGACY'
FROM projects project
WHERE budget.project_id = project.id;

ALTER TABLE budgets
  ALTER COLUMN proposed_project_name SET NOT NULL,
  DROP CONSTRAINT ck_budgets_status,
  ADD CONSTRAINT ck_budgets_status CHECK (status IN ('DRAFT', 'FINALIZED', 'APPROVED', 'REJECTED')),
  ADD CONSTRAINT ck_budgets_project_link_origin CHECK (project_link_origin IS NULL OR project_link_origin IN ('LEGACY', 'GENERATED')),
  ADD CONSTRAINT ck_budgets_approved_project CHECK (status <> 'APPROVED' OR project_id IS NOT NULL);
