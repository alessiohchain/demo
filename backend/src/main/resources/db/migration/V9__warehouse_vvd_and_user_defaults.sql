-- V9: seed the WarehouseFacility VVD + give the wcs user a default
-- facility / warehouse so csnx-ui's WarehouseSwitcher renders an
-- interactive dropdown (it switches into multi-option mode at
-- options.length > 1, and shows the current selection from the user
-- record).
--
-- WarehouseFacility codes follow csnx-ui's convention: a single string
-- combining "{facility} / {warehouse}" with a slash separator.

INSERT INTO demoschema.lookup_value (lookup_key, code, label, sort_order, created_at, updated_at, created_by)
VALUES
    ('WarehouseFacility', 'WCS-MAIN / WCS-WH1', 'Main facility, Warehouse 1',       0, NOW(), NOW(), 'system'),
    ('WarehouseFacility', 'WCS-MAIN / WCS-WH2', 'Main facility, Warehouse 2',       1, NOW(), NOW(), 'system'),
    ('WarehouseFacility', 'WCS-DR / WCS-WH3',   'Disaster recovery, Warehouse 3',   2, NOW(), NOW(), 'system');

UPDATE demoschema.app_user
SET default_facility = 'WCS-MAIN',
    default_warehouse = 'WCS-WH1'
WHERE cpy_cd = 'WCS' AND username = 'wcs';
