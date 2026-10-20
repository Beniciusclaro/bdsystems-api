CREATE TABLE compositions
(
  id                 UUID PRIMARY KEY,
  company_id         UUID                     NOT NULL REFERENCES company (id),
  code               VARCHAR(50)              NOT NULL,
  name               VARCHAR(180)             NOT NULL,
  description        TEXT,
  unit_of_measure_id UUID                     NOT NULL REFERENCES units_of_measure (id),
  base_yield         NUMERIC(19, 4)           NOT NULL DEFAULT 1,
  active             BOOLEAN                  NOT NULL DEFAULT TRUE,
  created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMP WITH TIME ZONE,
  CONSTRAINT uk_compositions_company_code UNIQUE (company_id, code),
  CONSTRAINT ck_compositions_yield CHECK (base_yield > 0)
);

CREATE TABLE composition_components
(
  id                   UUID PRIMARY KEY,
  composition_id       UUID           NOT NULL REFERENCES compositions (id) ON DELETE CASCADE,
  type                 VARCHAR(24)    NOT NULL,
  material_id          UUID REFERENCES materials (id),
  supplier_material_id UUID REFERENCES supplier_materials (id),
  labor_role_id        UUID REFERENCES labor_roles (id),
  tool_rental_id       UUID REFERENCES tool_rentals (id),
  description          VARCHAR(220),
  unit_label           VARCHAR(30),
  quantity             NUMERIC(19, 4) NOT NULL,
  custom_unit_cost     NUMERIC(19, 4),
  sort_order           INTEGER        NOT NULL DEFAULT 0,
  CONSTRAINT ck_composition_component_type CHECK (type IN ('MATERIAL', 'LABOR', 'EQUIPMENT', 'CUSTOM')),
  CONSTRAINT ck_composition_component_quantity CHECK (quantity > 0),
  CONSTRAINT ck_composition_component_cost CHECK (custom_unit_cost IS NULL OR custom_unit_cost >= 0),
  CONSTRAINT ck_composition_component_reference CHECK (
    (type = 'MATERIAL' AND material_id IS NOT NULL AND supplier_material_id IS NOT NULL AND labor_role_id IS NULL AND
     tool_rental_id IS NULL AND custom_unit_cost IS NULL)
      OR (type = 'LABOR' AND labor_role_id IS NOT NULL AND material_id IS NULL AND supplier_material_id IS NULL AND
          tool_rental_id IS NULL AND custom_unit_cost IS NULL)
      OR (type = 'EQUIPMENT' AND tool_rental_id IS NOT NULL AND material_id IS NULL AND supplier_material_id IS NULL AND
          labor_role_id IS NULL AND custom_unit_cost IS NULL)
      OR (type = 'CUSTOM' AND description IS NOT NULL AND unit_label IS NOT NULL AND custom_unit_cost IS NOT NULL AND
          material_id IS NULL AND supplier_material_id IS NULL AND labor_role_id IS NULL AND tool_rental_id IS NULL)
    )
);

CREATE INDEX idx_compositions_company_active ON compositions (company_id, active);
CREATE INDEX idx_composition_components_composition ON composition_components (composition_id, sort_order);
