-- V6: report_text table + 3 seed rows for the wcs/WCS user.
--
-- Mirrors CSnx's SCWT_REPORT_TEXT field-by-field (column names align with
-- the Java field names rather than CSnx's SCWT column abbreviations).
--
-- Conceptual reference:
--   C:\software\projects\CSnx\src\za\co\csnx\model\csnx\ReportText.java
--   composite PK (CPY_CD, REPORT_PROGRAM, SYSTEM_LANGUAGE, TEXT_ID)

CREATE TABLE demoschema.report_text (
    company_code        VARCHAR(8)   NOT NULL REFERENCES demoschema.company(cpy_cd),
    report_name         VARCHAR(64)  NOT NULL,
    language            VARCHAR(8)   NOT NULL,
    text_sequence       INTEGER      NOT NULL,
    report_text         TEXT         NOT NULL,
    maintenance_date    DATE         NOT NULL,
    maintenance_time    TIME         NOT NULL,
    maintenance_user    VARCHAR(64)  NOT NULL,
    maintenance_tran    VARCHAR(32)  NOT NULL,

    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    update_serial       BIGINT       NOT NULL DEFAULT 0,

    PRIMARY KEY (company_code, report_name, language, text_sequence)
);

CREATE INDEX idx_report_text_program ON demoschema.report_text (company_code, report_name);

-- Seed rows under company WCS, program DEMO_RPT, language EN.
INSERT INTO demoschema.report_text (
    company_code, report_name, language, text_sequence, report_text,
    maintenance_date, maintenance_time, maintenance_user, maintenance_tran,
    created_at, updated_at, created_by
) VALUES
    ('WCS', 'DEMO_RPT', 'EN', 1, 'Welcome to the demo report.',
        CURRENT_DATE, CURRENT_TIME, 'system', 'INSERT', NOW(), NOW(), 'system'),
    ('WCS', 'DEMO_RPT', 'EN', 2, 'This is the second line of the report.',
        CURRENT_DATE, CURRENT_TIME, 'system', 'INSERT', NOW(), NOW(), 'system'),
    ('WCS', 'DEMO_RPT', 'EN', 3, 'Final line — generated for the wcs user.',
        CURRENT_DATE, CURRENT_TIME, 'system', 'INSERT', NOW(), NOW(), 'system');

-- ReportProgram VVD — the metadata's `lookupDataKey='ReportProgram'` dropdown.
INSERT INTO demoschema.lookup_value (lookup_key, code, label, sort_order, created_at, updated_at, created_by)
VALUES
    ('ReportProgram', 'DEMO_RPT',   'Demo Report',    0, NOW(), NOW(), 'system'),
    ('ReportProgram', 'INVOICE',    'Invoice',        1, NOW(), NOW(), 'system'),
    ('ReportProgram', 'PACKLIST',   'Packing List',   2, NOW(), NOW(), 'system');
