CREATE TABLE tools
(
  id         UUID PRIMARY KEY,
  company_id UUID                     NOT NULL,
  code       VARCHAR(50)              NOT NULL,
  name       VARCHAR(150)             NOT NULL,

  active     BOOLEAN                  NOT NULL DEFAULT TRUE,

  created_at TIMESTAMP WITH TIME ZONE NOT NULL
                                               DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE,

  CONSTRAINT fk_tools_company
    FOREIGN KEY (company_id)
      REFERENCES company (id),

  CONSTRAINT uk_tools_company_code
    UNIQUE (company_id, code),

  -- Permite referenciar a ferramenta juntamente com o tenant.
  CONSTRAINT uk_tools_company_id_id
    UNIQUE (company_id, id)
);

CREATE TABLE tool_rentals
(
  id                UUID PRIMARY KEY,
  company_id        UUID                     NOT NULL,
  tool_id           UUID                     NOT NULL,
  supplier_id       UUID                     NOT NULL,

  price             NUMERIC(12, 2)           NOT NULL,
  rental_period     VARCHAR(20)              NOT NULL,

  rental_start_date DATE,
  rental_end_date   DATE,

  created_at        TIMESTAMP WITH TIME ZONE NOT NULL
    DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP WITH TIME ZONE,

  CONSTRAINT fk_tool_rentals_company
    FOREIGN KEY (company_id)
      REFERENCES company (id),

  CONSTRAINT fk_tool_rentals_tool
    FOREIGN KEY (company_id, tool_id)
      REFERENCES tools (company_id, id),

  CONSTRAINT fk_tool_rentals_supplier
    FOREIGN KEY (supplier_id)
      REFERENCES suppliers (id),

  CONSTRAINT ck_tool_rentals_price
    CHECK (price >= 0),

  CONSTRAINT ck_tool_rentals_period
    CHECK (
      rental_period IN (
                        'HOUR',
                        'DAY',
                        'WEEK',
                        'MONTH'
        )
      ),

  CONSTRAINT ck_tool_rentals_dates
    CHECK (
      rental_start_date IS NULL
        OR rental_end_date IS NULL
        OR rental_end_date >= rental_start_date
      )
);

CREATE INDEX idx_tools_company
  ON tools (company_id);

CREATE INDEX idx_tool_rentals_tool
  ON tool_rentals (company_id, tool_id);

CREATE INDEX idx_tool_rentals_supplier
  ON tool_rentals (supplier_id);

CREATE INDEX idx_tool_rentals_company_dates
  ON tool_rentals (
                   company_id,
                   rental_start_date,
                   rental_end_date
    );
