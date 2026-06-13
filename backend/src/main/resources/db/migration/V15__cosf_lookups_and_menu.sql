-- V15: TraderType VVD + COSF menu placement.
--
-- ScreenMetadataSeeder picks the `shortName: "COSF"` out of the JSON
-- resource file and stamps it as `screen_metadata.fastpath`. This migration
-- only adds the menu entry + the lookup data the new screens read.

INSERT INTO demoschema.lookup_value (lookup_key, code, label, sort_order, created_at, updated_at, created_by)
VALUES
    ('TraderType', 'W', 'Warehouse',  0, NOW(), NOW(), 'system'),
    ('TraderType', 'S', 'Supplier',   1, NOW(), NOW(), 'system'),
    ('TraderType', 'C', 'Customer',   2, NOW(), NOW(), 'system');

-- Place COSF in the menu tree as a top-level entry (no parent). The
-- fastpath itself is stamped on screen_metadata by ScreenMetadataSeeder
-- from the JSON file's `shortName`, but the menu_item row controls
-- visibility in the sidebar.
INSERT INTO demoschema.menu_item (code, label, fastpath, workflow, sort_order, created_at, updated_at, created_by)
VALUES
    ('COSF', 'Corporate Shipment Flows', 'COSF', 'corporateShipmentFlows', 100, NOW(), NOW(), 'system');
