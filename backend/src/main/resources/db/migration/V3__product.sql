-- V3: Product entity — the smoke-target for the metadata-driven UI engine.
CREATE TABLE demoschema.product (
    id              BIGSERIAL       PRIMARY KEY,
    sku             VARCHAR(64)     NOT NULL UNIQUE,
    name            VARCHAR(120)    NOT NULL,
    price_minor     BIGINT          NOT NULL DEFAULT 0,   -- price in cents
    active          BOOLEAN         NOT NULL DEFAULT TRUE,

    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    update_serial   BIGINT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_product_sku ON demoschema.product (sku);

-- Two seed rows so cmd_search has something to return on first paint.
INSERT INTO demoschema.product (sku, name, price_minor, active, created_at, updated_at, created_by)
VALUES
    ('WIDGET-001', 'Widget',        1999, TRUE,  NOW(), NOW(), 'system'),
    ('GIZMO-001',  'Gizmo Deluxe',  4999, TRUE,  NOW(), NOW(), 'system');
