CREATE TABLE units_of_measure (
    id UUID PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE material_categories (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_material_categories_company_name UNIQUE(company_id, name)
);
CREATE TABLE materials (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    unit_of_measure_id UUID NOT NULL REFERENCES units_of_measure(id),
    category_id UUID REFERENCES material_categories(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_materials_company_code UNIQUE(company_id, code)
);
CREATE TABLE supplier_materials (
    id UUID PRIMARY KEY,
    material_id UUID NOT NULL REFERENCES materials(id),
    supplier_id UUID NOT NULL REFERENCES suppliers(id),
    supplier_code VARCHAR(100),
    reference_price NUMERIC(12,2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_supplier_materials_material_supplier UNIQUE(material_id, supplier_id),
    CONSTRAINT ck_supplier_materials_price CHECK(reference_price >= 0)
);
CREATE INDEX idx_materials_unit ON materials(unit_of_measure_id);
CREATE INDEX idx_materials_category ON materials(category_id);
CREATE INDEX idx_materials_company_active ON materials(company_id, active);
CREATE INDEX idx_materials_company_name ON materials(company_id, name);
CREATE INDEX idx_material_categories_company_active ON material_categories(company_id, active);
CREATE INDEX idx_supplier_materials_supplier ON supplier_materials(supplier_id);
