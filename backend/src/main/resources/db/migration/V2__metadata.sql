-- V2: metadata + lookup + menu tables for the UI engine.
--
-- screen_metadata holds the full MetadataHolder JSON for each workflow.
-- The engine reads it via GET /api/metadata/screen?workflow=<id> and renders
-- forms/grids/toolbars from the payload — one row per workflow, no SWET-style
-- relational decomposition.
--
-- lookup_value drives the dropdown VVD map returned in the login bundle
-- (engine reads it as Record<lookupKey, Record<code, label>>).
--
-- menu_item drives the navigation tree returned in the same bundle.

CREATE TABLE demoschema.screen_metadata (
    id              BIGSERIAL       PRIMARY KEY,
    workflow        VARCHAR(120)    NOT NULL UNIQUE,
    fastpath        VARCHAR(16)     UNIQUE,
    payload         JSONB           NOT NULL,
    description     TEXT,

    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    update_serial   BIGINT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_screen_metadata_workflow ON demoschema.screen_metadata (workflow);
CREATE INDEX idx_screen_metadata_fastpath ON demoschema.screen_metadata (fastpath);

CREATE TABLE demoschema.lookup_value (
    lookup_key      VARCHAR(64)     NOT NULL,
    code            VARCHAR(64)     NOT NULL,
    label           VARCHAR(255)    NOT NULL,
    sort_order      INTEGER         NOT NULL DEFAULT 0,

    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    update_serial   BIGINT          NOT NULL DEFAULT 0,

    PRIMARY KEY (lookup_key, code)
);

CREATE TABLE demoschema.menu_item (
    id              BIGSERIAL       PRIMARY KEY,
    parent_id       BIGINT          REFERENCES demoschema.menu_item(id) ON DELETE CASCADE,
    code            VARCHAR(64)     NOT NULL UNIQUE,
    label           VARCHAR(120)    NOT NULL,
    fastpath        VARCHAR(16),
    workflow        VARCHAR(120),
    sort_order      INTEGER         NOT NULL DEFAULT 0,

    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    update_serial   BIGINT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_menu_item_parent ON demoschema.menu_item (parent_id);

-- Seed a single demoPing screen so the dispatcher has something to call.
-- The payload is a minimal MetadataHolder with one form (no fields), one
-- toolbar, and a "ping" command. ProductActivityService in Phase C will
-- replace this with a real workflow.
INSERT INTO demoschema.screen_metadata (
    workflow, fastpath, payload, description,
    created_at, updated_at, created_by
) VALUES (
    'demoPing',
    'PING',
    jsonb_build_object(
        'name', 'demoPing',
        'shortName', 'PING',
        'version', 1,
        'metadataList', jsonb_build_array(
            jsonb_build_object(
                'kind', 'toolbar',
                'name', 'demoPingToolbar',
                'actionButtons', jsonb_build_array(
                    jsonb_build_object(
                        'id', 'cmd_ping',
                        'name', 'cmd_ping',
                        'value', 'Ping',
                        'command', 'cmd_ping',
                        'modelType', 'none'
                    )
                )
            )
        )
    ),
    'Phase B smoke target — minimal screen the engine can fetch and the dispatcher can echo against.',
    NOW(), NOW(), 'system'
);
