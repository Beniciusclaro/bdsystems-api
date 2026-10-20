CREATE TABLE suppliers
(
  id            UUID PRIMARY KEY,
  company_id    UUID                     NOT NULL,

  legal_name    VARCHAR(255)             NOT NULL,
  trade_name    VARCHAR(255),
  tax_number    VARCHAR(50),

  email         VARCHAR(255),
  phone_number  VARCHAR(50),
  website       VARCHAR(255),

  address_id    UUID                     NOT NULL UNIQUE,

  contact_name  VARCHAR(255),
  payment_terms VARCHAR(255),
  notes         TEXT,

  active        BOOLEAN                  NOT NULL DEFAULT TRUE,

  created_at    TIMESTAMP WITH TIME ZONE NOT NULL
                                                  DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP WITH TIME ZONE,

  CONSTRAINT fk_suppliers_company
    FOREIGN KEY (company_id) REFERENCES company (id),

  CONSTRAINT fk_suppliers_address
    FOREIGN KEY (address_id) REFERENCES addresses (id),

  CONSTRAINT uk_suppliers_company_tax_number
    UNIQUE (company_id, tax_number)
);
