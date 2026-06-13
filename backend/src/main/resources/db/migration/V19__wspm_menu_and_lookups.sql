-- V19: YON (Yes/No) VVD + Admin parent menu + WSPM placement.
--
-- The WSPM screen's many boolean flags render as combobox-style dropdowns
-- backed by the YON lookup family. The fastpath itself is stamped on
-- screen_metadata by ScreenMetadataSeeder from
-- sysParameters.maintenance.json's `shortName: "WSPM"`; this migration
-- adds the lookup rows + menu entry.

-- ─── YON (Yes / No) lookup family ─────────────────────────────────
INSERT INTO demoschema.lookup_value (lookup_key, code, label, sort_order, created_at, updated_at, created_by)
VALUES
    ('YON', 'Y', 'Yes', 0, NOW(), NOW(), 'system'),
    ('YON', 'N', 'No',  1, NOW(), NOW(), 'system');

-- ─── Admin parent menu ────────────────────────────────────────────
INSERT INTO demoschema.menu_item (code, label, sort_order, created_at, updated_at, created_by)
VALUES ('admin', 'Admin', 30, NOW(), NOW(), 'system');

-- ─── WSPM child menu, nested under Admin ─────────────────────────
INSERT INTO demoschema.menu_item (code, label, fastpath, workflow, parent_id, sort_order, created_at, updated_at, created_by)
VALUES (
    'WSPM',
    'System Parameters',
    'WSPM',
    'sysParameters.maintenance',
    (SELECT id FROM demoschema.menu_item WHERE code = 'admin'),
    10,
    NOW(), NOW(), 'system'
);
