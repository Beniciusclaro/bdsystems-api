CREATE TABLE clients
(
  id         UUID PRIMARY KEY,
  company_id UUID                     NOT NULL REFERENCES company (id),
  type       VARCHAR(16)              NOT NULL,
  name       VARCHAR(180)             NOT NULL,
  document   VARCHAR(32),
  email      VARCHAR(180),
  phone      VARCHAR(40),
  street     VARCHAR(180),
  city       VARCHAR(100),
  state      VARCHAR(100),
  zip_code   VARCHAR(20),
  active     BOOLEAN                  NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE,
  CONSTRAINT uk_clients_company_document UNIQUE (company_id, document),
  CONSTRAINT ck_clients_type CHECK (type IN ('INDIVIDUAL', 'COMPANY'))
);

CREATE TABLE projects
(
  id                 UUID PRIMARY KEY,
  company_id         UUID                     NOT NULL REFERENCES company (id),
  client_id          UUID                     NOT NULL REFERENCES clients (id),
  code               VARCHAR(50)              NOT NULL,
  name               VARCHAR(180)             NOT NULL,
  description        TEXT,
  street             VARCHAR(180),
  city               VARCHAR(100),
  state              VARCHAR(100),
  zip_code           VARCHAR(20),
  planned_start_date DATE,
  planned_end_date   DATE,
  status             VARCHAR(24)              NOT NULL DEFAULT 'PLANNING',
  active             BOOLEAN                  NOT NULL DEFAULT TRUE,
  created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMP WITH TIME ZONE,
  CONSTRAINT uk_projects_company_code UNIQUE (company_id, code),
  CONSTRAINT ck_projects_dates CHECK (planned_end_date IS NULL OR planned_start_date IS NULL OR
                                      planned_end_date >= planned_start_date),
  CONSTRAINT ck_projects_status CHECK (status IN ('PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE labor_roles
(
  id                 UUID PRIMARY KEY,
  company_id         UUID                     NOT NULL REFERENCES company (id),
  code               VARCHAR(50)              NOT NULL,
  name               VARCHAR(150)             NOT NULL,
  description        TEXT,
  costing_mode       VARCHAR(24)              NOT NULL DEFAULT 'EMPLOYEE_AVERAGE',
  manual_hourly_cost NUMERIC(19, 4),
  active             BOOLEAN                  NOT NULL DEFAULT TRUE,
  created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMP WITH TIME ZONE,
  CONSTRAINT uk_labor_roles_company_code UNIQUE (company_id, code),
  CONSTRAINT ck_labor_role_mode CHECK (costing_mode IN ('EMPLOYEE_AVERAGE', 'MANUAL')),
  CONSTRAINT ck_labor_role_manual_cost CHECK (manual_hourly_cost IS NULL OR manual_hourly_cost >= 0)
);

CREATE TABLE employee_labor_costs
(
  id                       UUID PRIMARY KEY,
  employee_id              UUID                     NOT NULL UNIQUE REFERENCES employees (id),
  labor_role_id            UUID                     NOT NULL REFERENCES labor_roles (id),
  productive_hours_month   NUMERIC(10, 2)           NOT NULL,
  work_days_month          NUMERIC(10, 2),
  employer_charges_percent NUMERIC(7, 4)            NOT NULL DEFAULT 0,
  monthly_benefits         NUMERIC(19, 4)           NOT NULL DEFAULT 0,
  other_monthly_costs      NUMERIC(19, 4)           NOT NULL DEFAULT 0,
  created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at               TIMESTAMP WITH TIME ZONE,
  CONSTRAINT ck_employee_labor_hours CHECK (productive_hours_month > 0),
  CONSTRAINT ck_employee_labor_days CHECK (work_days_month IS NULL OR work_days_month > 0),
  CONSTRAINT ck_employee_labor_charges CHECK (employer_charges_percent >= 0),
  CONSTRAINT ck_employee_labor_benefits CHECK (monthly_benefits >= 0 AND other_monthly_costs >= 0)
);

CREATE INDEX idx_clients_company_active ON clients (company_id, active);
CREATE INDEX idx_projects_company_client ON projects (company_id, client_id);
CREATE INDEX idx_labor_roles_company_active ON labor_roles (company_id, active);
CREATE INDEX idx_employee_labor_role ON employee_labor_costs (labor_role_id);
