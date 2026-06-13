-- V20: drop demo's local UI-metadata tables. The platform DB is now the single
-- store: demo registers its screens/menus/lookups there on boot (module_cd=DEMO)
-- and reads them back via PlatformMetadataSource (the classpath manifest is the
-- offline fallback). Nothing in demo reads these tables any more — the engine
-- path and the §16 grant lookups all source from the central manifest. The
-- app_user table stays as a module-local profile cache (facility/warehouse
-- defaults); its now-unused password/lockout columns are harmless and can be
-- trimmed in a later migration alongside the AppUser entity.

DROP TABLE IF EXISTS demoschema.screen_metadata;
DROP TABLE IF EXISTS demoschema.menu_item;
DROP TABLE IF EXISTS demoschema.lookup_value;
