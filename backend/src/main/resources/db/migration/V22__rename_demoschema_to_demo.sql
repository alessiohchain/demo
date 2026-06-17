-- V22: cleanup — rename schema demoschema -> demo (drop the redundant "schema"
-- suffix from the schema name).
--
-- Flyway's history table lives in public.flyway_schema_history (default), so the
-- rename is safe — no migration metadata moves with it. Every entity's
-- @Table(schema = "demo") and the @SequenceGenerator sequence name are updated
-- to match. ALTER SCHEMA RENAME relocates the schema's sequences too, so the
-- shipment-flow sequence moves with it. This module has no functions/triggers
-- whose bodies hardcode the schema. Mirrors pom's V23 precedent.

ALTER SCHEMA demoschema RENAME TO demo;
