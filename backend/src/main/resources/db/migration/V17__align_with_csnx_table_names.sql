-- V17: Align the four CSnx-modelled tables (RPTM / COSF / CSFD / Trader)
-- with CSnx's canonical table + column names so future ports of CSnx
-- activities don't need column-name juggling. Postgres convention is
-- lowercase, so CSnx's UPPER_CASE names become lowercase here.
--
-- Decisions (recorded in the conversation that produced this migration):
--   * Match CSnx column names exactly (cpy_cd, maint_*, update_serial,
--     report_program, system_language, text_id).
--   * Use CSnx table prefixes (scct_ / scwt_).
--   * Keep BaseEntity's audit columns (created_at, updated_at, created_by,
--     updated_by) as demo extensions on top of the CSnx shape.
--   * Trader stays simplified — column-name aligned but the full SCWT_TRADER
--     30+ column schema is out of scope.
--
-- Postgres preserves FK constraints across RENAME TABLE and RENAME COLUMN;
-- no need to drop/recreate the shipment_flow_detail → header FK.

-- ============================================================
-- shipment_flow_header → scct_shipment_flow_header
-- ============================================================
ALTER TABLE demoschema.shipment_flow_header RENAME TO scct_shipment_flow_header;

ALTER TABLE demoschema.scct_shipment_flow_header
    RENAME COLUMN company_code     TO cpy_cd;
ALTER TABLE demoschema.scct_shipment_flow_header
    RENAME COLUMN maintenance_date TO maint_date;
ALTER TABLE demoschema.scct_shipment_flow_header
    RENAME COLUMN maintenance_time TO maint_time;
ALTER TABLE demoschema.scct_shipment_flow_header
    RENAME COLUMN maintenance_user TO maint_user;
ALTER TABLE demoschema.scct_shipment_flow_header
    RENAME COLUMN maintenance_tran TO maint_tran;

ALTER TABLE demoschema.scct_shipment_flow_header
    ALTER COLUMN cpy_cd     TYPE VARCHAR(15),
    ALTER COLUMN maint_user TYPE VARCHAR(30),
    ALTER COLUMN maint_user DROP NOT NULL,
    ALTER COLUMN maint_tran TYPE VARCHAR(30),
    ALTER COLUMN maint_tran DROP NOT NULL,
    ALTER COLUMN maint_date SET DEFAULT CURRENT_DATE,
    ALTER COLUMN maint_time SET DEFAULT CURRENT_TIME,
    ALTER COLUMN update_serial SET DEFAULT 1;

-- ============================================================
-- shipment_flow_detail → scct_shipment_flow_detail
-- ============================================================
ALTER TABLE demoschema.shipment_flow_detail RENAME TO scct_shipment_flow_detail;

ALTER TABLE demoschema.scct_shipment_flow_detail
    RENAME COLUMN company_code     TO cpy_cd;
ALTER TABLE demoschema.scct_shipment_flow_detail
    RENAME COLUMN maintenance_date TO maint_date;
ALTER TABLE demoschema.scct_shipment_flow_detail
    RENAME COLUMN maintenance_time TO maint_time;
ALTER TABLE demoschema.scct_shipment_flow_detail
    RENAME COLUMN maintenance_user TO maint_user;
ALTER TABLE demoschema.scct_shipment_flow_detail
    RENAME COLUMN maintenance_tran TO maint_tran;

ALTER TABLE demoschema.scct_shipment_flow_detail
    ALTER COLUMN cpy_cd     TYPE VARCHAR(15),
    ALTER COLUMN maint_user TYPE VARCHAR(30),
    ALTER COLUMN maint_user DROP NOT NULL,
    ALTER COLUMN maint_tran TYPE VARCHAR(30),
    ALTER COLUMN maint_tran DROP NOT NULL,
    ALTER COLUMN maint_date SET DEFAULT CURRENT_DATE,
    ALTER COLUMN maint_time SET DEFAULT CURRENT_TIME,
    ALTER COLUMN update_serial SET DEFAULT 1;

ALTER INDEX demoschema.idx_shipment_flow_detail_parent
    RENAME TO idx_scct_shipment_flow_detail_parent;
ALTER SEQUENCE demoschema.shipment_flow_detail_shipment_flow_id_seq
    RENAME TO scct_shipment_flow_detail_shipment_flow_id_seq;

-- ============================================================
-- trader → scwt_trader
-- ============================================================
ALTER TABLE demoschema.trader RENAME TO scwt_trader;

ALTER TABLE demoschema.scwt_trader
    RENAME COLUMN company_code     TO cpy_cd;
ALTER TABLE demoschema.scwt_trader
    RENAME COLUMN maintenance_date TO maint_date;
ALTER TABLE demoschema.scwt_trader
    RENAME COLUMN maintenance_time TO maint_time;
ALTER TABLE demoschema.scwt_trader
    RENAME COLUMN maintenance_user TO maint_user;
ALTER TABLE demoschema.scwt_trader
    RENAME COLUMN maintenance_tran TO maint_tran;

ALTER TABLE demoschema.scwt_trader
    ALTER COLUMN cpy_cd     TYPE VARCHAR(15),
    ALTER COLUMN maint_user TYPE VARCHAR(30),
    ALTER COLUMN maint_user DROP NOT NULL,
    ALTER COLUMN maint_tran TYPE VARCHAR(30),
    ALTER COLUMN maint_tran DROP NOT NULL,
    ALTER COLUMN maint_date SET DEFAULT CURRENT_DATE,
    ALTER COLUMN maint_time SET DEFAULT CURRENT_TIME,
    ALTER COLUMN update_serial SET DEFAULT 1;

ALTER INDEX demoschema.idx_trader_name RENAME TO idx_scwt_trader_name;

-- ============================================================
-- report_text → scwt_report_text
-- ============================================================
ALTER TABLE demoschema.report_text RENAME TO scwt_report_text;

ALTER TABLE demoschema.scwt_report_text
    RENAME COLUMN company_code     TO cpy_cd;
ALTER TABLE demoschema.scwt_report_text
    RENAME COLUMN report_name      TO report_program;
ALTER TABLE demoschema.scwt_report_text
    RENAME COLUMN language         TO system_language;
ALTER TABLE demoschema.scwt_report_text
    RENAME COLUMN text_sequence    TO text_id;
ALTER TABLE demoschema.scwt_report_text
    RENAME COLUMN maintenance_date TO maint_date;
ALTER TABLE demoschema.scwt_report_text
    RENAME COLUMN maintenance_time TO maint_time;
ALTER TABLE demoschema.scwt_report_text
    RENAME COLUMN maintenance_user TO maint_user;
ALTER TABLE demoschema.scwt_report_text
    RENAME COLUMN maintenance_tran TO maint_tran;

-- CSnx SCWT_REPORT_TEXT keeps CPY_CD at VARCHAR(8) and maint_user / maint_tran
-- as VARCHAR(30) NOT NULL (different from SCCT family). Match that.
ALTER TABLE demoschema.scwt_report_text
    ALTER COLUMN maint_user TYPE VARCHAR(30),
    ALTER COLUMN maint_tran TYPE VARCHAR(30),
    ALTER COLUMN update_serial SET DEFAULT 1;

ALTER INDEX demoschema.idx_report_text_program RENAME TO idx_scwt_report_text_program;
