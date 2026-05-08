-- =============================================================================
-- V1__create_products_table.sql - Product Service database migration
-- =============================================================================

CREATE TABLE IF NOT EXISTS products (
    id              VARCHAR(36)      NOT NULL,
    name            VARCHAR(255)     NOT NULL,
    sku             VARCHAR(50)      NOT NULL,
    description     TEXT,
    price           NUMERIC(12, 2)   NOT NULL,
    stock_quantity  INTEGER          NOT NULL DEFAULT 0,
    category        VARCHAR(100)     NOT NULL,
    image_url       VARCHAR(500),
    active          BOOLEAN          NOT NULL DEFAULT TRUE,
    brand           VARCHAR(100),
    weight_grams    INTEGER,
    average_rating  NUMERIC(3, 2)    NOT NULL DEFAULT 0.00,
    review_count    INTEGER          NOT NULL DEFAULT 0,

    -- Audit fields
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(36),
    updated_by      VARCHAR(36),
    version         BIGINT           NOT NULL DEFAULT 0,

    CONSTRAINT pk_products          PRIMARY KEY (id),
    CONSTRAINT uq_products_sku      UNIQUE (sku),
    CONSTRAINT chk_products_price   CHECK (price > 0),
    CONSTRAINT chk_products_stock   CHECK (stock_quantity >= 0),
    CONSTRAINT chk_products_rating  CHECK (average_rating >= 0 AND average_rating <= 5)
);

CREATE INDEX IF NOT EXISTS idx_products_sku      ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_active   ON products(active);
CREATE INDEX IF NOT EXISTS idx_products_price    ON products(price);
CREATE INDEX IF NOT EXISTS idx_products_brand    ON products(brand);
CREATE INDEX IF NOT EXISTS idx_products_created_at ON products(created_at DESC);

-- Auto-update updated_at trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Seed sample products for testing
INSERT INTO products (id, name, sku, description, price, stock_quantity, category, brand, active, version, created_at, updated_at)
VALUES
    ('prod-id-0001-0000-0000-000000000001', 'iPhone 15 Pro', 'ELEC-00001', 'Apple iPhone 15 Pro 256GB Space Black', 134999.00, 50, 'Electronics', 'Apple', true, 0, NOW(), NOW()),
    ('prod-id-0002-0000-0000-000000000002', 'Samsung Galaxy S24', 'ELEC-00002', 'Samsung Galaxy S24 Ultra 512GB', 124999.00, 30, 'Electronics', 'Samsung', true, 0, NOW(), NOW()),
    ('prod-id-0003-0000-0000-000000000003', 'Laptop Dell XPS 15', 'COMP-00001', 'Dell XPS 15 Intel i9 32GB RAM 1TB SSD', 189999.00, 15, 'Computers', 'Dell', true, 0, NOW(), NOW()),
    ('prod-id-0004-0000-0000-000000000004', 'Nike Air Max 2024', 'SHOE-00001', 'Nike Air Max running shoes size 10', 12999.00, 100, 'Footwear', 'Nike', true, 0, NOW(), NOW()),
    ('prod-id-0005-0000-0000-000000000005', 'Spring Boot in Action', 'BOOK-00001', 'Spring Boot in Action by Craig Walls', 2499.00, 200, 'Books', 'Manning', true, 0, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

COMMENT ON TABLE products IS 'Product catalog for the e-commerce platform';
COMMENT ON COLUMN products.sku IS 'Unique stock keeping unit identifier';
COMMENT ON COLUMN products.stock_quantity IS 'Available units; never negative; managed by Order Service';
COMMENT ON COLUMN products.active IS 'Soft delete: false = removed from catalog';
