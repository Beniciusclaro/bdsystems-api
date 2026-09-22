CREATE TABLE addresses
(
    id       UUID PRIMARY KEY,
    street   VARCHAR(255) NOT NULL,
    city     VARCHAR(255) NOT NULL,
    state    VARCHAR(255) NOT NULL,
    zip_code VARCHAR(255) NOT NULL
);

CREATE TABLE company
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(150)             NOT NULL,
    address_id UUID                     NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_company_address
        FOREIGN KEY (address_id) REFERENCES addresses (id)
);

CREATE TABLE users
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL UNIQUE,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(255) NOT NULL,
    address_id UUID         NOT NULL UNIQUE,
    company_id UUID         NOT NULL,

    CONSTRAINT fk_users_address
        FOREIGN KEY (address_id) REFERENCES addresses (id),

    CONSTRAINT fk_users_company
        FOREIGN KEY (company_id) REFERENCES company (id)
);

CREATE INDEX idx_users_company_id ON users (company_id);