-- CSNX-14756 Phase 2 — DEMO hosts its own CDC capture config.
--
-- The engine band creates int_cdc_capture in every module (V9026); this puts DEMO's
-- own row in DEMO's copy, so LocalCdcConfigSource can drive the poller from the
-- module's own schema instead of fetching it from the platform over Channel 1.
--
-- The identical row still exists in platform.int_cdc_capture and keeps serving the
-- remote fetch until every module has cut over; CSNX-14757 Phase 3 deletes it there.
-- Both copies carry the same config_name on purpose — it is the key int_cdc_state
-- and int_cdc_run_state join on, and DEMO's backfill marker is already written
-- against 'demo-trader'. Changing it here would re-backfill the whole trader table.
--
-- created_at / updated_at are supplied EXPLICITLY. The engine band declares them
-- NOT NULL with no column DEFAULT, deliberately: the timestamps are stamped from
-- Java by JPA auditing, so only the screen path gets them for free and a Flyway
-- seed must stamp its own. NOW() inside an INSERT is fine; it is the column DEFAULT
-- that the band bans. cpy_cd, enabled and log_enabled do keep their defaults.
--
-- Idempotent by config_name so the seed is safe on a database that already has the
-- row. Portable, and that takes the odd-looking FROM: no ON CONFLICT (postgres-only),
-- and a bare SELECT of literals with no FROM is rejected by DB2. (VALUES(1)) is the
-- same one-row dummy source the engine's own claim uses on both vendors. The engine
-- ships a db2 twin of this table, so the seed has to be able to follow it there.

INSERT INTO demo.int_cdc_capture
    (cpy_cd, module_cd, config_name, enabled, source_table, event_type,
     subject_columns, notes, log_enabled, created_at, updated_at, created_by)
SELECT 'WCS', 'DEMO', 'demo-trader', TRUE, 'demo.scwt_trader', 'demo.trader.changed',
       'cpy_cd,trader_type,trader_code',
       'Trader master -> pom.int_trader mirror (WAL CDC)', TRUE,
       NOW(), NOW(), 'system'
  FROM (VALUES(1)) AS dual(x)
 WHERE NOT EXISTS (
    SELECT 1 FROM demo.int_cdc_capture WHERE config_name = 'demo-trader'
);
