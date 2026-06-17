# Database migrations

Flyway conventions for demo. Companion to `dao-patterns.md` (the Java side)
and `engine.md` §17 (which describes how screen JSON resources interact
with Flyway).

## File layout

```
backend/src/main/resources/db/migration/
    V1__init.sql
    V2__metadata.sql
    ...
    V17__align_with_csnx_table_names.sql
```

Naming: `V<n>__<snake_case_description>.sql`. Two underscores between the
version and the description. Version numbers are strictly ascending; gaps
are fine but never reuse a number.

**Released migrations are immutable.** A migration that has run on any
environment cannot be edited — write a new one. Flyway's checksum
validation will refuse to start the app if the file's hash drifts from
the recorded one.

## Schema is always `demo`

Every table is fully qualified: `CREATE TABLE demo.foo (...)`. The
backend's `application.yml` sets `spring.flyway.default-schema=demo`
+ `spring.jpa.properties.hibernate.default_schema=demo` so JPA finds
the tables without per-entity `schema=` annotations on every `@Table`.

When cloning the template for a new module, change the schema name to the
module's name (`<module>`) in both `application.yml` and every
existing migration **before** the first run.

## CSnx-aligned column shapes

Tables that mirror legacy CSnx (the `scct_*` and `scwt_*` families) follow
CSnx's column conventions, lowercased for Postgres:

| CSnx column | Demo column | Type | Notes |
|---|---|---|---|
| `CPY_CD` | `cpy_cd` | `VARCHAR(15) NOT NULL` | Company-scope key. `scwt_report_text` is the one exception — `VARCHAR(8)` to match CSnx exactly. |
| `MAINT_DATE` | `maint_date` | `DATE NOT NULL DEFAULT CURRENT_DATE` | Auto-stamped by `@PrePersist` on save (see `dao-patterns.md`); default protects against direct SQL inserts. |
| `MAINT_TIME` | `maint_time` | `TIME NOT NULL DEFAULT CURRENT_TIME` | Same. |
| `MAINT_USER` | `maint_user` | `VARCHAR(30)` (nullable) | Stamped by `applyAuditStamps` in the activity. |
| `MAINT_TRAN` | `maint_tran` | `VARCHAR(30)` (nullable) | Default is the verb (`INSERT`/`UPDATE`/`DELETE`); activities can override. |
| `UPDATE_SERIAL` | `update_serial` | `BIGINT NOT NULL DEFAULT 1` | JPA `@Version` column. Default 1 protects against direct inserts; JPA increments from 0 on its own. |

Native demo tables (auth, config, lookup) don't follow this naming —
they use plain `created_at` / `updated_at` etc. The CSnx column convention
applies to CSnx-modelled tables only.

## BaseEntity audit columns

On top of the `maint_*` columns above, every table whose entity extends
`BaseEntity` also carries:

```sql
created_at    TIMESTAMPTZ NOT NULL,
updated_at    TIMESTAMPTZ NOT NULL,
created_by    VARCHAR(100),
updated_by    VARCHAR(100),
update_serial BIGINT      NOT NULL DEFAULT 1,
```

`created_*` / `updated_*` are written by Spring Data's
`AuditingEntityListener` — they capture the same audit trail at the
framework level. CSnx's `MAINT_*` is the per-row business audit; Spring's
`created_at` / `updated_at` is the persistence-layer audit. Both
coexist deliberately.

## What Flyway ships and what it doesn't

**Flyway migrations carry:**

- Table DDL — `CREATE TABLE`, `ALTER TABLE`, `CREATE INDEX`, FKs, sequences.
- Reference data — `INSERT` into `lookup_value` (VVD dropdowns), `menu_item`,
  role/permission rows, fastpath rows.
- One-off data fixes / backfills.

**Flyway migrations do NOT carry:**

- Screen metadata payloads. Those live as JSON resources under
  `backend/src/main/resources/screens/<workflow>.json` and are upserted
  by `ScreenMetadataSeeder` on every boot (the seeder hashes each
  payload to short-circuit unchanged files). Iterating on a screen =
  edit JSON + restart. No new Flyway file needed unless the screen ships
  a **new fastpath** (which means a new row in `menu_item` and a role
  grant — both Flyway-shipped) or a **new lookup-value family** for a
  VVD on that screen.

The split keeps screen iterations cheap: no migration version bump per
field rename.

**Updating a screen JSON requires a backend image rebuild, not just a
restart.** The resources live inside the Spring Boot fat-jar (baked at
`mvn package` time), so `docker compose restart backend` reuses the
existing image — `ScreenMetadataSeeder` happily re-seeds the same old
payload. Always use `docker compose up --build -d backend` after a
JSON edit. (Local dev outside Docker is fine: `./mvnw spring-boot:run`
picks up changes via classpath scanning on restart.)

## Worked-example migration history

`V1__init.sql` through `V17__align_with_csnx_table_names.sql` cover the
demo's evolution end-to-end. Notable entries:

| Migration | Adds |
|---|---|
| `V1__init.sql` | Schema, base auth tables, `lookup_value`, `screen_metadata`. |
| `V5__multi_company_auth.sql` | Multi-tenant company table + user/company link. |
| `V6__report_text.sql` | RPTM table (first CSnx-modelled entity). |
| `V11__rptm_metadata_from_csnx.sql` | Screen-metadata row for `reportText.maintenance` (predates the JSON-resource seeder; later migrations rely on the seeder). |
| `V12__screen_metadata_payload_hash.sql` | Adds `payload_hash` column so the seeder can skip unchanged payloads. |
| `V13__shipment_flow_tables.sql` | COSF + CSFD header/detail tables. |
| `V14__trader_and_details.sql` | Trader master + seed traders. |
| `V15__cosf_lookups_and_menu.sql` | `TraderType` VVD + COSF menu entry. |
| `V16__corporate_menu_parent.sql` | Corporate parent menu (now houses COSF). |
| `V17__align_with_csnx_table_names.sql` | Rename `shipment_flow_*` / `trader` / `report_text` to `scct_*` / `scwt_*` and `company_code` / `maintenance_*` to `cpy_cd` / `maint_*` so the four CSnx-modelled tables match CSnx's column shape exactly. |

`V17` is the one to read first if you're aligning a new CSnx port —
it captures every column-name + type translation rule in one place.

## Boot ordering guarantees

The compose stack enforces this order:

1. **`postgres`** comes up; healthcheck waits for it to accept connections.
2. **`backend`** starts; Flyway runs all pending migrations against
   `demo`. If a migration fails the container exits non-zero.
3. **`ScreenMetadataSeeder`** runs as a `@PostConstruct` hook after
   Flyway. Hashes each `screens/*.json` resource, upserts changed ones,
   warns on workflows in the DB without a matching file (drift signal —
   manual SQL crept in).
4. **`frontend`** comes up last (nginx is happy regardless of backend
   state; the `/api` reverse-proxy returns 502 until the backend is
   live, but the React shell still loads).

This means: a new screen iteration is "edit JSON, restart backend
container" — no Flyway run needed. A new table is "write a `V<n+1>` +
restart backend container" — Flyway picks it up on the next boot.

## When to write a new migration

| Change | Migration? |
|---|---|
| Edit a screen JSON | No — restart backend, seeder upserts. |
| Add a new screen JSON | No (same as above) **unless** the screen registers a fastpath or needs new VVD lookup rows — then yes for the menu + lookup rows. |
| Add a new table / column / index | Yes. |
| Add a new VVD lookup family | Yes — `INSERT` into `lookup_value`. |
| Add a new menu entry | Yes — `INSERT` into `menu_item`. |
| Backfill / one-off data fix | Yes. |
| Rename a column on a released table | Yes — write a new `V<n+1>`. Never edit the original migration. |

## Anti-patterns

- **Don't** edit a released migration. Flyway will refuse to boot if the
  checksum changes.
- **Don't** put screen-metadata JSON payloads into a migration's INSERT
  statement. The seeder is the source of truth; SQL-inserted screen rows
  drift the moment someone edits the JSON.
- **Don't** drop and recreate a table to "fix" its definition — write a
  proper `ALTER`. Drop/recreate loses every row.
- **Don't** issue raw `psql` against the demo DB to add a table. Future
  environments boot from migrations + JSON only; anything you do live
  vanishes on the next clean start.

## Related

- **`dao-patterns.md`** for the Java-side mapping (entity hierarchy,
  `@Version updateSerial`, `@IdClass`, sequence generators).
- **`activity-services.md`** for how an activity stamps the `maint_*`
  columns above.
- **`engine.md`** §17 (lookup bootstrap) for how `lookup_value` rows
  reach the frontend.
- **`architecture.md`** §Containers & deployment for the full boot
  sequence diagram.
