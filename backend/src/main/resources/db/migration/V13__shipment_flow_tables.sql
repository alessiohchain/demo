-- V13: Corporate Shipment Flow header + detail tables (COSF + CSFD).
--
-- Mirrors CSnx's SCCT_SHIPMENT_FLOW_HEADER + SCCT_SHIPMENT_FLOW_DETAIL.
-- Composite PKs are spelt out to match the engine's `idFromData` reconstruction.
--
-- Conceptual reference:
--   C:\software\projects\CSnx\src\za\co\csnx\model\csnx\ScctShipmentFlowHeader.java
--   C:\software\projects\CSnx\src\za\co\csnx\model\csnx\ScctShipmentFlowDetail.java
--   migration  20210611122832_TRANSHIP_CSNX-8197_Shipment_Flow.sql

CREATE TABLE demoschema.shipment_flow_header (
    company_code        VARCHAR(8)   NOT NULL REFERENCES demoschema.company(cpy_cd),
    shipment_flow       VARCHAR(30)  NOT NULL,
    flow_description    VARCHAR(100) NOT NULL,
    maintenance_date    DATE         NOT NULL,
    maintenance_time    TIME         NOT NULL,
    maintenance_user    VARCHAR(64)  NOT NULL,
    maintenance_tran    VARCHAR(32)  NOT NULL,

    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    update_serial       BIGINT       NOT NULL DEFAULT 0,

    PRIMARY KEY (company_code, shipment_flow)
);

CREATE TABLE demoschema.shipment_flow_detail (
    company_code         VARCHAR(8)   NOT NULL,
    shipment_flow        VARCHAR(30)  NOT NULL,
    shipment_flow_id     BIGSERIAL    NOT NULL,
    shipment_flow_seq    INTEGER      NOT NULL,
    trader_type          VARCHAR(9)   NOT NULL DEFAULT 'W',
    trader_code          VARCHAR(45)  NOT NULL,
    transit_time         INTEGER      NOT NULL DEFAULT 0,
    process_time         INTEGER      NOT NULL DEFAULT 0,
    maintenance_date     DATE         NOT NULL,
    maintenance_time     TIME         NOT NULL,
    maintenance_user     VARCHAR(64)  NOT NULL,
    maintenance_tran     VARCHAR(32)  NOT NULL,

    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    update_serial        BIGINT       NOT NULL DEFAULT 0,

    PRIMARY KEY (company_code, shipment_flow, shipment_flow_id),
    FOREIGN KEY (company_code, shipment_flow)
        REFERENCES demoschema.shipment_flow_header (company_code, shipment_flow)
        ON DELETE RESTRICT
);

CREATE INDEX idx_shipment_flow_detail_parent
    ON demoschema.shipment_flow_detail (company_code, shipment_flow);

-- Seed: two headers so COSF has data on first boot. Details are seeded in
-- V14 after the trader table exists (FK target).
INSERT INTO demoschema.shipment_flow_header (
    company_code, shipment_flow, flow_description,
    maintenance_date, maintenance_time, maintenance_user, maintenance_tran,
    created_at, updated_at, created_by
) VALUES
    ('WCS', 'A1',     'Main',                   CURRENT_DATE, CURRENT_TIME, 'system', 'INSERT', NOW(), NOW(), 'system'),
    ('WCS', 'STORE1', 'Test through a store',   CURRENT_DATE, CURRENT_TIME, 'system', 'INSERT', NOW(), NOW(), 'system');
