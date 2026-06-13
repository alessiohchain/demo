-- V8: drop the Phase A/B/C scaffolding now that the Phase D screens, auth,
-- and entities have replaced it. Runs AFTER V5–V7 introduce the new tables
-- and after the Java code stops referencing the dropped entities.

DELETE FROM demoschema.screen_metadata
WHERE workflow IN ('demoPing', 'productMaintenance');

DELETE FROM demoschema.menu_item
WHERE code IN ('product-maintenance');

DROP TABLE IF EXISTS demoschema.product;
DROP TABLE IF EXISTS demoschema.customer;
