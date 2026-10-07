CREATE TABLE employees
(
    id               UUID PRIMARY KEY,
    name             VARCHAR(255) NOT NULL UNIQUE,
    email            VARCHAR(255) NOT NULL UNIQUE,
    phone_number     VARCHAR(255) NOT NULL UNIQUE,
    document_number  VARCHAR(255),
    birth_date       DATE,
    address_id       UUID         NOT NULL UNIQUE,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_date     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP,
    company_id       UUID         NOT NULL,
    employee_number  VARCHAR(100),
    position         VARCHAR(150) NOT NULL,
    department       VARCHAR(150),
    admission_date   DATE,
    termination_date DATE,
    status           VARCHAR(32),

    CONSTRAINT fk_employees_address
        FOREIGN KEY (address_id) REFERENCES addresses (id),
    CONSTRAINT fk_employees_company
        FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT uk_employees_company_number
        UNIQUE (company_id, employee_number)
);
