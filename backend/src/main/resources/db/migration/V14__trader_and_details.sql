-- V14: trader table (for the trader.prompt picker) + seed traders + seed
-- detail rows that reference them.
--
-- Conceptual reference:
--   C:\software\projects\CSnx\src\za\co\csnx\model\csnx\ScctTrader.java
--   composite PK (CPY_CD, TRADER_TYPE, TRADER_CODE)

CREATE TABLE demoschema.trader (
    company_code        VARCHAR(8)   NOT NULL REFERENCES demoschema.company(cpy_cd),
    trader_type         VARCHAR(9)   NOT NULL,
    trader_code         VARCHAR(45)  NOT NULL,
    trader_name         VARCHAR(100) NOT NULL,
    maintenance_date    DATE         NOT NULL,
    maintenance_time    TIME         NOT NULL,
    maintenance_user    VARCHAR(64)  NOT NULL,
    maintenance_tran    VARCHAR(32)  NOT NULL,

    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    update_serial       BIGINT       NOT NULL DEFAULT 0,

    PRIMARY KEY (company_code, trader_type, trader_code)
);

CREATE INDEX idx_trader_name ON demoschema.trader (company_code, trader_name);

-- Seed: a handful of demo traders mixing the three TraderType codes
-- (W=Warehouse, S=Supplier, C=Customer).
INSERT INTO demoschema.trader (
    company_code, trader_type, trader_code, trader_name,
    maintenance_date, maintenance_time, maintenance_user, maintenance_tran,
    created_at, updated_at, created_by
) VALUES
    ('WCS', 'W', 'WH-101',  'Warehouse 101 — Cork',         CURRENT_DATE, CURRENT_TIME, 'system', 'INSERT', NOW(), NOW(), 'system'),
    ('WCS', 'W', 'WH-510',  'Warehouse 510 — Dublin',       CURRENT_DATE, CURRENT_TIME, 'system', 'INSERT', NOW(), NOW(), 'system'),
    ('WCS', 'W', 'WH-110',  'Warehouse 110 — Limerick',     CURRENT_DATE, CURRENT_TIME, 'system', 'INSERT', NOW(), NOW(), 'system'),
    ('WCS', 'S', 'ACME',    'ACME Industrial Supplies',     CURRENT_DATE, CURRENT_TIME, 'system', 'INSERT', NOW(), NOW(), 'system'),
    ('WCS', 'S', 'NORTH',   'Northwind Traders',            CURRENT_DATE, CURRENT_TIME, 'system', 'INSERT', NOW(), NOW(), 'system'),
    ('WCS', 'C', 'C-001',   'Customer One Retail',          CURRENT_DATE, CURRENT_TIME, 'system', 'INSERT', NOW(), NOW(), 'system'),
    ('WCS', 'C', 'C-002',   'Customer Two Wholesale',       CURRENT_DATE, CURRENT_TIME, 'system', 'INSERT', NOW(), NOW(), 'system'),
    ('WCS', 'C', 'C-003',   'Customer Three Online',        CURRENT_DATE, CURRENT_TIME, 'system', 'INSERT', NOW(), NOW(), 'system');

-- Seed: detail rows for the A1 header so CSFD has something to display.
INSERT INTO demoschema.shipment_flow_detail (
    company_code, shipment_flow, shipment_flow_seq,
    trader_type, trader_code, transit_time, process_time,
    maintenance_date, maintenance_time, maintenance_user, maintenance_tran,
    created_at, updated_at, created_by
) VALUES
    ('WCS', 'A1', 10, 'W', 'WH-101', 1, 0, CURRENT_DATE, CURRENT_TIME, 'system', 'INSERT', NOW(), NOW(), 'system'),
    ('WCS', 'A1', 20, 'W', 'WH-510', 2, 1, CURRENT_DATE, CURRENT_TIME, 'system', 'INSERT', NOW(), NOW(), 'system'),
    ('WCS', 'A1', 30, 'C', 'C-001',  0, 0, CURRENT_DATE, CURRENT_TIME, 'system', 'INSERT', NOW(), NOW(), 'system');
