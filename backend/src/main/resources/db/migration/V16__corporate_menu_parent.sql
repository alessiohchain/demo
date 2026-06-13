-- V16: introduce a "Corporate" parent menu item and nest COSF under it.
-- Until now COSF rendered as a top-level sidebar entry; future corporate-
-- domain screens (transfer flows, shipment plans, …) will share this
-- subtree.

INSERT INTO demoschema.menu_item (code, label, sort_order, created_at, updated_at, created_by)
VALUES ('corporate', 'Corporate', 20, NOW(), NOW(), 'system');

UPDATE demoschema.menu_item
SET parent_id = (SELECT id FROM demoschema.menu_item WHERE code = 'corporate'),
    sort_order = 10
WHERE code = 'COSF';
