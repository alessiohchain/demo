# Entity & repository conventions

How JPA entities and Spring Data repositories are laid out in demo.
Companion to `activity-services.md` (which describes how activities
consume them) and `migrations.md` (which describes the DB-side shape).

## The entity inheritance chain

```
BaseEntity                     @MappedSuperclass; audit + @Version updateSerial
  │
  └── MaintainableDateTimeBaseEntity         @PrePersist/@PreUpdate stamp maint date+time
        │
        └── MaintainableTranUserBaseEntity   declares MaintainableTranUser (tran + user)
              │
              ├── ShipmentFlowHeader
              ├── ShipmentFlowDetail
              ├── Trader
              └── ReportText
```

`MaintainableDateTime` and `MaintainableTranUser` interfaces mirror the
same names in legacy CSnx — porting an entity from CSnx means dropping
the column annotations into a class that extends the right level. The
auto-stamping behaviour is identical.

**Pick the level by what columns the table has:**

| Table has… | Entity extends |
|---|---|
| Just `update_serial` + Spring Data audit columns | `BaseEntity` |
| `maint_date` + `maint_time` only | `MaintainableDateTimeBaseEntity` |
| `maint_date` + `maint_time` + `maint_tran` + `maint_user` | `MaintainableTranUserBaseEntity` |

In practice the four CSnx-modelled tables all carry the full set →
`MaintainableTranUserBaseEntity`. Config/lookup/auth tables
(`AppUser`, `Company`, `MenuItem`, `ScreenMetadata`) extend
`BaseEntity` directly because they're demo-native, not CSnx-aligned.

**Exception — `LookupValue`** is a flat `@Entity` with inline audit columns
(historical; predates the BaseEntity hierarchy). Don't follow that pattern
for new entities; extend the right superclass.

**No per-entity `equals`/`hashCode` or `setNullToDefaults`.** Both are
CSnx workarounds for a legacy schema demo doesn't share. Demo
migrations declare DB-side `DEFAULT` clauses on every `NOT NULL`
column, and entities use JPA identity equality (composite-key `Pk`
classes carry their own `equals`/`hashCode` for `findById`). See
[decisions.md § 9](decisions.md#9-entities-skip-csnxs-per-entity-equalshashcode-and-setnulltodefaults)
for the full rationale + when to revisit.

## `update_serial` is the optimistic-lock column

Every persisted-from-Java table must carry `update_serial BIGINT NOT NULL`
(see `migrations.md`). The Java field is `updateSerial`, declared once on
`BaseEntity`:

```java
@Version
@Column(name = "update_serial", nullable = false)
private Long updateSerial;
```

Subclasses inherit it. **Never override or re-declare it on a subclass** —
that breaks JPA's version tracking and concurrent updates silently clobber
each other.

If you ever need to add a new entity that for some reason can't extend
`BaseEntity`, paste the `@Version` block verbatim. The example in
`LookupValue` shows this.

## Composite PKs use `@IdClass`

CSnx-aligned tables typically have multi-column PKs (e.g.
`(companyCode, shipmentFlow, shipmentFlowId)`). The convention is:

- Declare each PK column with `@Id` directly on the entity.
- Add `@IdClass(Foo.Pk.class)` on the entity.
- The `Pk` class is a nested `static class` (named `Pk` to avoid shadowing
  `@Id`), implements `Serializable`, declares the same fields with
  same names, has a no-arg + all-args constructor, plus `equals` /
  `hashCode` derived from the PK fields.

Use `@IdClass` **not** `@EmbeddedId` — consistency across the codebase
matters more than which one is technically nicer in isolation.

Examples: `ShipmentFlowHeader.Pk`, `ShipmentFlowDetail.Pk`, `Trader.Pk`,
`ReportText.Pk`, `LookupValue.Key` (named `Key` for historical reasons —
new ones use `Pk`).

## DB-generated keys on composite PKs need `@SequenceGenerator`

Hibernate rejects `GenerationType.IDENTITY` on a column that's part of a
composite `@IdClass`. Use `GenerationType.SEQUENCE` with a
`@SequenceGenerator` pointing at the matching Postgres sequence
(BIGSERIAL columns auto-create one named `<table>_<column>_seq`):

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shipment_flow_detail_seq")
@SequenceGenerator(
        name = "shipment_flow_detail_seq",
        sequenceName = "demo.scct_shipment_flow_detail_shipment_flow_id_seq",
        allocationSize = 1)
@Column(name = "shipment_flow_id", nullable = false)
private Long shipmentFlowId;
```

`ShipmentFlowDetail` is the worked example. `allocationSize = 1` keeps the
sequence in lockstep with the column (no Hibernate pre-allocation) so manual
`INSERT` statements + JPA inserts can coexist without skipping ids.

## Repositories — one extension point

`BaseRepository<T, ID extends Serializable>` is the only repository base in
the codebase:

```java
@NoRepositoryBean
public interface BaseRepository<T, ID extends Serializable>
        extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
    T findByIdOrThrow(ID id);
}
```

Every domain repository extends `BaseRepository` — never `JpaRepository`
directly. This gives every repo:

- The full `JpaRepository` surface (save, findAll, findById, count, etc.).
- `JpaSpecificationExecutor` for typed dynamic queries when finder methods
  get out of hand.
- `findByIdOrThrow` for the common "fetch or 404" pattern.

**No custom DAOs, no `@PersistenceContext`, no `EntityManager` injection
in services.** All data access goes through a repository. The activity
services and domain services hold repository references; they don't reach
past them to JPA primitives.

## Never load rows and then reduce them in Java

**Filtering, the row limit and the offset go into the query** — a `WHERE`, a
derived `findTop…`/`existsBy…`/`countBy…`, a `Specification`, Spring Data's
`Limit` or `Pageable`, `setFirstResult`/`setMaxResults`, or
`OFFSET … FETCH FIRST` in native SQL. Paging passes the **start index and the
number of rows** to the query; it never fetches everything and slices. A
`findAll(…)` / `getResultList()` / `queryForList(…)` whose result is then
`.stream().filter(…)`, `.limit(…)`, `.skip(…)`, `.findFirst()` or
`.subList(…)` reads rows the database could have left behind — every row of a
table that only grows, on every call. The two acceptable Java narrowings:

- dropping the one look-ahead row a capped search fetches (`limit + 1`, so it
  knows "more" without a `COUNT`);
- a redundant safety net over a filter that is **already** in the query.

Reading a whole table is fine only when the table is small configuration and the
code genuinely needs all of it — a cache snapshot, a lookup bundle — and then
plain iteration, not a narrowing stream. Grid searches go through the engine's
`searchForGrid`, which puts the scope, the offset and the cap in SQL; an
override that narrows rows puts that narrowing in the scope it passes, never in
a post-filter.

## JDBC is the exception

**Application code uses JPA repositories; test code may use JDBC freely.** Plain
JDBC in `src/main` (`JdbcTemplate`, `NamedParameterJdbcTemplate`,
`JdbcClient`, a raw `DataSource`/`Connection`) is allowed only for one of
these reasons, and **the class says which in a `JDBC:` line of its javadoc**
(`<p><b>JDBC:</b> …`):

1. a startup guard or Flyway callback that runs before JPA exists;
2. a Flyway Java migration;
3. DDL or catalogue inspection (`DatabaseMetaData`, `SYSCAT`);
4. a committed compare-and-swap claim, or an insert-if-absent, whose **rowcount**
   is the contract;
5. a `REQUIRES_NEW` append that must survive the caller's rollback;
6. a CDC/WAL reader, an **event mirror store** (CDC or outbox change events applied as
   idempotent `INSERT … ON CONFLICT` upserts to an entity-free read-model —
   `platform/docs/cdc-event-integration.md` §"Step 3 — Add the consumer", item 3),
   or a table only known at runtime;
7. a retention purge (one set-based `DELETE`);
8. a framework store (Spring Authorization Server's JDBC services);
9. a LOB deliberately kept off the entity, read by key;
10. the engine's entity-free runtime — stores that must work in a module that
    does not `@EntityScan` the engine;
11. wiring only — a `@Configuration` that hands the template to one of the above.

The list is the fleet's and is closed: the canonical copy is
`platform/docs/dao-patterns.md` §"JDBC is the exception". A class that fits none
of the reasons moves to JPA — "it was quicker to write" is not a reason. A
module that needs a twelfth reason raises it for the fleet rather than adding it
here. Multi-row JDBC reads are bounded in SQL like any other, and native SQL
through JPA (`createNativeQuery`, `@Query(nativeQuery = true)`) is held to the
same rule.

**Both rules are enforced** by `DataAccessTest`, which calls the engine's
`DataAccessScanner`: a JDBC import with no `JDBC:` reason, or a
load-then-reduce chain, fails the ordinary `mvnw test`. The load-then-reduce
check is a regex, so it sees the one-statement shape only — the rule still
applies to the split form it cannot see.

## Naming finder methods

Spring Data derived finder methods follow standard convention
(`findByCompanyCodeOrderByShipmentFlow`). Long method names are fine —
they're readable at the call site. When a derived method gets unwieldy
(more than ~4 predicates), switch to `@Query` JPQL or a
`Specification<T>` — never inline a `EntityManager.createQuery` in the
service.

For "find the max value of a column" use `@Query` with `MAX()` rather
than `findTopByOrderBy<X>Desc` — clearer intent, one less DB round trip.
See `ShipmentFlowDetailRepository.findMaxShipmentFlowSeq`.

## Java field names ≠ column names

Postgres convention is lowercase, CSnx convention is `UPPER_CASE`. The
Java field stays camelCase; the column annotation maps it:

```java
@Column(name = "cpy_cd", length = 15, nullable = false)
private String companyCode;
```

Same Java field name across CSnx and demo entities; only the
`@Column(name = …)` changes. This keeps activity services + DTO mappers
identical across both projects.

## Bean layer: business logic + `@Transactional` boundary

Activity services live in `za.co.csnx.demo.service.activity`; activity
beans (interface + impl + base) live in
`za.co.csnx.demo.business.activity`. Mirrors CSnx's
`server.services.activity` / `business.activity` split.

Activity services don't inject repositories. They inject the
**`XxxActivity` interface** (not the `XxxActivityBean` impl), and the
bean owns:

- The repository field (one repository per bean).
- The `@Transactional` annotation — **always at the method level,
  never at the class level**. Each public method on a `CrudActivity`
  impl declares its own `@Transactional` or `@Transactional(readOnly =
  true)`. Mirrors CSnx; the read/write boundary stays explicit at every
  call site and survives method extraction.
- `idOf(entity)` — public on the interface; services call it for
  cmd_update / cmd_delete PK derivation.
- `validate(entity, mode)` — **real business rules only**. DB-level
  `NOT NULL` constraints handle "X is required" rejections; don't
  duplicate them. Only enforce ranges, cross-field, lookup-existence.
- `beforeSave(entity, mode)` — default-value population, screen-tran
  stamping.
- Probe-based `search(probe)` — default impl on
  `AbstractCrudActivityBean` matches the probe's non-null bean properties
  **in SQL** (`ProbeQuery`, engine-spring 0.3.43+). It is not row-limited;
  grids use `searchForGrid`, which adds the scope, offset and cap.
  `csnx.engine.search.sql.enabled=false` restores the old in-memory filter
  over `findAll()` — a breach of §"Never load rows and then reduce them in
  Java", kept only as a fallback.
- Screen-specific finders (e.g. `TraderActivity.namesByCompany`) live
  on the interface + impl when they don't fit `search`'s shape.

Mirrors CSnx's `XxxActivity` interface + `XxxActivityBean` impl split.
Spring DI resolves the interface to the sole `@Service`-annotated
impl. No `BusinessHelper` facade — beans that need other beans
constructor-inject those interfaces directly (CSFD's bean injects
`TraderActivity`, not `TraderActivityBean`).

**Rules:**

- Beans are **interface + impl**. Services and cross-bean wirings
  reference the interface type.
- No `*ActivityService` class touches a repository directly. Grep
  `backend/src/main/java/za/co/csnx/demo/engine` for `repository\.` —
  every match should be inside `*ActivityBean.java`.
- **`@Transactional` is method-level only.** No class-level
  annotation on beans.
- **No `idFromData` / field-by-field PK extraction.** Build the whole
  entity from the wire via `fromData(data, entity)` (PK fields
  included), derive the PK via `bean.idOf(entity)`. Mirrors CSnx's
  `copyDynamicModel` pattern.
- **Validation = real business rules.** DB constraints handle
  presence; bean validates ranges, cross-field, lookups.

See [activity-services.md](activity-services.md) for the
service-vs-bean responsibility table and worked examples.

## Porting an entity from CSnx

CSnx entities live at `C:\software\projects\CSnx\src\za\co\csnx\model\csnx\`
with PK classes in `model/csnx/keys/` and the base classes
(`BaseEntity`, `MaintainableDateTimeBaseEntity`,
`MaintainableTranUserBaseEntity`) in `model/`. Demo's chain mirrors
CSnx's — porting is a translation, not a redesign:

1. Pick the same superclass demo-side as the CSnx entity uses CSnx-side.
2. Keep every Java field name unchanged (`companyCode`, `shipmentFlow`,
   `maintenanceDate`, etc.). Only `@Column(name = …)` and
   `@Table(name = …)` get lowercased.
3. Copy the PK class to a nested `static class Pk` with the same fields
   and `equals`/`hashCode`.
4. Drop CSnx-isms (`OpenJPAEntityManager`, `synchParentRelationships`,
   metamodel classes, `setNullToDefaults`, `getString`/`getLong` value
   wrappers) — Hibernate doesn't need them. Demo's `BaseEntity` is
   intentionally lean.
5. If the CSnx table has columns demo doesn't yet model, decide whether
   to port them now or defer — but never silently drop them; record
   the decision in a comment.

See `Trader.java` (simplified from CSnx's 30+ column SCWT_TRADER) and
`ShipmentFlowHeader.java` (full port) as the two ends of the spectrum.

**When a ported screen using this entity misbehaves, suspect the
backend first.** Demo's frontend engine was lifted from csnx-ui and is
already exercised by every working screen plus the Playwright suite.
If csnx-ui handles the equivalent screen correctly, the bug is almost
always entity-side (column mapping, audit columns missing, composite-PK
reconstruction) or activity-side (response envelope, parent-context
plumbing) — not engine-side. Reproduce against the live stack, compare
the `/api/process` traffic to csnx-ui's, and fix the backend mismatch
before touching engine code. See `activity-services.md` for the
analogous rule on the activity side.

## Related

- **`activity-services.md`** for how activities consume repositories +
  entities.
- **`migrations.md`** for the matching DDL conventions (`update_serial`
  defaults, `maint_*` types, schema, sequence naming).
- **`architecture.md`** §Backend for where these packages sit in the
  module layout.
