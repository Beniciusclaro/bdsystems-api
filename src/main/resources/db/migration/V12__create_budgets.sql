CREATE TABLE budgets
(
  id                       UUID PRIMARY KEY,
  company_id               UUID                     NOT NULL REFERENCES company (id),
  client_id                UUID                     NOT NULL REFERENCES clients (id),
  project_id               UUID                     NOT NULL REFERENCES projects (id),
  number                   VARCHAR(50)              NOT NULL,
  title                    VARCHAR(180)             NOT NULL,
  valid_until              DATE,
  commercial_terms         TEXT,
  notes                    TEXT,
  tax_percent              NUMERIC(7, 4)            NOT NULL DEFAULT 0,
  margin_percent           NUMERIC(7, 4)            NOT NULL DEFAULT 0,
  status                   VARCHAR(20)              NOT NULL DEFAULT 'DRAFT',
  client_name_snapshot     VARCHAR(180),
  client_document_snapshot VARCHAR(32),
  project_name_snapshot    VARCHAR(180),
  project_address_snapshot VARCHAR(500),
  finalized_at             TIMESTAMP WITH TIME ZONE,
  created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at               TIMESTAMP WITH TIME ZONE,
  CONSTRAINT uk_budgets_company_number UNIQUE (company_id, number),
  CONSTRAINT ck_budgets_status CHECK (status IN ('DRAFT', 'FINALIZED')),
  CONSTRAINT ck_budgets_percent CHECK (tax_percent >= 0 AND margin_percent >= 0 AND tax_percent + margin_percent < 100)
);

CREATE TABLE budget_stages
(
  id          UUID PRIMARY KEY,
  budget_id   UUID         NOT NULL REFERENCES budgets (id) ON DELETE CASCADE,
  name        VARCHAR(180) NOT NULL,
  description TEXT,
  sort_order  INTEGER      NOT NULL DEFAULT 0
);

CREATE TABLE budget_items
(
  id                        UUID PRIMARY KEY,
  stage_id                  UUID           NOT NULL REFERENCES budget_stages (id) ON DELETE CASCADE,
  source_composition_id     UUID REFERENCES compositions (id),
  composition_code_snapshot VARCHAR(50),
  name                      VARCHAR(180)   NOT NULL,
  description               TEXT,
  unit_symbol               VARCHAR(20)    NOT NULL,
  quantity                  NUMERIC(19, 4) NOT NULL,
  unit_cost                 NUMERIC(19, 4) NOT NULL,
  total_cost                NUMERIC(19, 4) NOT NULL,
  unit_sale_price           NUMERIC(19, 4) NOT NULL,
  total_sale_price          NUMERIC(19, 4) NOT NULL,
  sort_order                INTEGER        NOT NULL DEFAULT 0,
  CONSTRAINT ck_budget_item_values CHECK (quantity > 0 AND unit_cost >= 0 AND total_cost >= 0 AND
                                          unit_sale_price >= 0 AND total_sale_price >= 0)
);

CREATE TABLE budget_item_components
(
  id                  UUID PRIMARY KEY,
  budget_item_id      UUID           NOT NULL REFERENCES budget_items (id) ON DELETE CASCADE,
  type                VARCHAR(24)    NOT NULL,
  description         VARCHAR(220)   NOT NULL,
  unit_label          VARCHAR(30)    NOT NULL,
  quantity            NUMERIC(19, 4) NOT NULL,
  unit_cost           NUMERIC(19, 4) NOT NULL,
  total_cost          NUMERIC(19, 4) NOT NULL,
  source_reference_id UUID,
  sort_order          INTEGER        NOT NULL DEFAULT 0,
  CONSTRAINT ck_budget_component_type CHECK (type IN ('MATERIAL', 'LABOR', 'EQUIPMENT', 'CUSTOM')),
  CONSTRAINT ck_budget_component_values CHECK (quantity > 0 AND unit_cost >= 0 AND total_cost >= 0)
);

CREATE INDEX idx_budgets_company_status ON budgets (company_id, status);
CREATE INDEX idx_budget_stages_budget_order ON budget_stages (budget_id, sort_order);
CREATE INDEX idx_budget_items_stage_order ON budget_items (stage_id, sort_order);
CREATE INDEX idx_budget_item_components_item ON budget_item_components (budget_item_id, sort_order);
