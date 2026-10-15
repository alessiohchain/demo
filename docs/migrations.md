# Database migrations — Demo

Fleet rules: [platform/docs/migrations.md](../../platform/docs/migrations.md) —
timestamp versions, [released migrations are immutable](../../platform/docs/migrations.md#released-migrations-are-immutable),
the [undo-script rule](../../platform/docs/migrations.md#every-migration-ships-an-undo-script),
portable SQL, boot ordering, anti-patterns, and the CSnx-modelled tables'
[worked-example history](../../platform/docs/migrations.md#worked-example-migration-history)
(which is demo's). This page holds only what is specific to Demo.

## Values

| | |
|---|---|
| Schema | `demo` — literal in every script and `@Table`; `APP_SCHEMA` relocates the engine band only ([why](../../platform/docs/migrations.md#schema-is-the-modules-own)) |
| Frozen sequential band | `V1`–`V23` (`LEGACY_SEQUENTIAL_THROUGH = 23`); everything new is `V<yyyyMMddHHmmss>` |
| Undo required from | `V20260808000000` (`UNDO_REQUIRED_FROM`) and anything above `V23` — `UndoScriptCoverageTest` |
| Vendors | postgres only (no `db2/` twins); the compose image is postgres 16 + wal2json with `wal_level=logical` for WAL CDC |
| Engine band | `V9000+` from `classpath:db/engine`, landed in `demo` via `${appSchema}` |
| Metadata side | registrar consumer — screens, `registry/menu.json` and `registry/lookups*.json` register with the platform store on boot; grants are platform-side. No metadata in Flyway since `V20`. |
| Trunk (what "released" means) | `main` |

## Module-only invariants

- **Scripts before `V22` say `demoschema`.** `V22__rename_demoschema_to_demo.sql`
  renamed the schema; the earlier scripts are released and stay as written —
  never "tidy" them to `demo`.
- **`demo.scwt_trader` is a CDC-captured table** (`V20260922090000` seeds its
  capture row; slot `csnx_cdc_demo`, event `demo.trader.changed`, consumed by
  pom). A column change to it is a change to that event's payload — check the
  consumer side ([cdc-event-integration.md](../../platform/docs/cdc-event-integration.md)).
- **`int_*` tables are integration read-models** written by the event
  consumers (`int_company` from `platform.company.changed`, `int_vendor` from
  `pom.vendor.changed`); non-PK columns are nullable on purpose and deletes are
  tombstoned.

## History worth knowing

| Migration | Why read it |
|---|---|
| `V17__align_with_csnx_table_names.sql` | Every CSnx column-name + type translation rule in one place — read first when porting a CSnx table. |
| `V20__drop_local_metadata.sql` | Drops the local `screen_metadata` / `menu_item` / `lookup_value` when demo became a registrar consumer. |
| `V22__rename_demoschema_to_demo.sql` | The schema rename (mirrors pom's V23). |
| `V23__int_vendor.sql` | The last sequential number — it landed in the same change as the first timestamped script (`V20260717121000`), which is why the frozen band ends at 23, not 22. |
