# Activity services — authoring guide

How to write a metadata-engine activity service in demo. This doc covers
the **authoring pattern**; the wire-level contract (request/response shape,
modelType routing, validation cascade) lives in `engine.md`.

## When you need one

Every workflow seeded in `screen_metadata` needs a Spring-managed bean that
implements `ActivityService`, registered under the same `workflow()` id.
`ProcessController` looks the bean up by workflow on every `/api/process`
call and dispatches the verb (`cmd_search`, `cmd_create`, etc.) to the
matching method.

The engine hierarchy is **three layers** — service → bean → repository.
Beans split into interface + impl per CSnx:

```
ActivityService (interface)
  └── AbstractEngineActivity            ← helpers, response factories, wire-shape hooks
        ├── AbstractCrudActivityService ← 5-verb CRUD dispatch (delegates to bean)
        │     ├── ReportTextActivityService          → ReportTextActivity (interface)
        │     ├── CorporateShipmentFlowActivityService → ShipmentFlowHeaderActivity + ShipmentFlowDetailActivity
        │     ├── CorporateShipmentFlowDetailsActivityService → ShipmentFlowDetailActivity
        │     └── TraderPromptActivityService        → TraderActivity
        └── SysParametersActivityService             → SysParametersActivity

CrudActivity<T, ID> (interface)                ← public bean API
  └── ReportTextActivity / TraderActivity / ShipmentFlowHeaderActivity / ShipmentFlowDetailActivity / SysParametersActivity

AbstractCrudActivityBean<T, ID> (impl base)    ← business logic
  └── ReportTextActivityBean implements ReportTextActivity
      TraderActivityBean implements TraderActivity
      ShipmentFlowHeaderActivityBean implements ShipmentFlowHeaderActivity
      ShipmentFlowDetailActivityBean implements ShipmentFlowDetailActivity
      SysParametersActivityBean implements SysParametersActivity

BaseRepository<T, ID> (Spring Data)            ← data access only
```

Mirrors CSnx's split: `BaseActivityService → XxxActivity (interface) → XxxActivityBean (impl) → DAO`.
Services + cross-bean wirings constructor-inject the `XxxActivity`
**interface** type, never the `XxxActivityBean` impl. CSFD's bean
injects `TraderActivity`, not `TraderActivityBean`, for trader-name
hydration + existence checks.

No `BusinessHelper` facade — Spring DI resolves the interface to the
sole `@Service`-annotated impl.

**What lives where:**

| Concern | Layer |
|---|---|
| Wire shape (`toData`/`fromData`), command routing | Service |
| Auth-context extraction (`currentCompanyCode`, `currentUsername`) | Service |
| Audit stamping (`applyAuditStamps`) | Service |
| Domain validation (`validate`), pre-save hooks (`beforeSave`) | Bean |
| PK extraction (`idOf`), probe-based search (`search`), custom finders | Bean |
| Repository access (`findById`, `save`, `existsById`, …) | **Bean only** |
| `@Transactional` (always method-level — never class) | Bean |

No `*ActivityService` class touches a repository directly. A grep of
`backend/src/main/java/za/co/csnx/demo/engine` for `repository\.` should
only match `*ActivityBean.java` files.

- **CRUD-shaped screens** (RPTM, COSF, CSFD, TRDP) extend
  `AbstractCrudActivityService<T, ID>` — it owns the 5-verb dispatch
  and delegates to the injected `CrudActivity<T, ID>` interface.
- **Non-CRUD screens** (singleton edit, custom inquiries, wizards)
  extend `AbstractEngineActivity` directly. WSPM is the canonical
  example; it injects `SysParametersActivity` and uses
  `loadOrDefault` + `save` from the base interface.

**Bean-layer rule: `@Transactional` is method-level only.** Every
public method on a `CrudActivity` impl declares its own
`@Transactional` or `@Transactional(readOnly = true)`. No class-level
annotation. Mirrors CSnx; keeps the boundary visible at each call
site, survives method extraction.

## The required hooks

`AbstractCrudActivityService` is a template-method class. A subclass
tells it how to translate between the engine's flat data map and the
JPA entity:

| Hook | Returns | Purpose |
|---|---|---|
| `workflow()` | `String` | The id that matches `screen_metadata.workflow` and the bean's registration key. |
| `toData(T)` | `Map<String,Object>` | Serialise an entity to the flat data map the engine renders. **Has a default** — the base class reflection-discovers every readable bean property on the entity's class hierarchy (cached per class), so most activities don't override. Override when the entity carries sensitive columns or needs a custom shape. |
| `fromData(Map, T)` | `void` | Populate `T` from the wire data map **including PK fields**. **Has a default** — the base class's `fromDataAuto` reflection-discovers each writable bean property, coerces the JSON value to the setter's parameter type, and invokes the setter. Audit + version columns (`companyCode`, `maint_*`, `updateSerial`, `created*` / `updated*`, `rowID`) are always skipped. Mirrors CSnx's `copyDynamicModel`. |
| `applyCompanyScope(T, companyCode)` | `void` | Stamp the user's company on the entity (default no-op — override on company-scoped entities). |

Constructor passes `Class<T>` to the base so `newInstance()` /
`newScoped(companyCode)` can reflect on the no-arg constructor. No
per-service `newEntity()` override.

That's all that's strictly required. The base class handles
`cmdSearch` / `cmdCreate` / `cmdUpdate` / `cmdDelete` / `cmdCopy` /
`cmdFilter` end-to-end.

**Note on PK extraction:** there is no per-service `idFromData(Map,
companyCode)` hook — the engine builds the whole entity from the wire
via `fromData` (PK fields included) and the bean's `idOf(entity)`
yields the PK. Same pattern for search: the base service builds a
probe entity from the criteria map and calls `bean.search(probe)`.

## Optional extension points

**On the service (`AbstractCrudActivityService`):**

| Hook | When to override |
|---|---|
| `findForSearch(companyCode, request)` | Override only for **parent-context injection** (e.g. CSFD reads `shipmentFlow` from `parentModel` and seeds it on the probe). The default builds a probe entity from the criteria map and calls `bean.search(probe)` — every non-master-detail screen uses this default. |
| `applyAuditStamps(T, username, tran)` | Almost never needed — the default writes `MAINT_USER` / `MAINT_TRAN` via `MaintainableTranUser`, and date/time stamping is handled by the entity's `@PrePersist`/`@PreUpdate`. |
| `cmdSearch` / `cmdCreate` / `cmdUpdate` / `cmdDelete` / `cmdCopy` | Override the verb directly when you need to reshape the response envelope (see "Response envelope shapes" below). |
| `cmdCustom(request)` | Define one when the screen has buttons whose `command` is not a standard verb (e.g. `cmd_details` on COSF). Dispatch by `request.command()`. |

**On the bean (`AbstractCrudActivityBean`):**

| Hook | When to override |
|---|---|
| `idOf(T)` | **Required** — extract the PK. Used by services (cmd_update / cmd_delete) and by `insert` for the duplicate-key check. |
| `validate(T entity, ValidationMode mode)` | **Real business rules only** — ranges, cross-field, lookup-existence. Don't duplicate DB `NOT NULL` constraints with "X is required" throws; the database handles that. Mirrors CSnx's `WaddActivityBean.isValid` surface. |
| `beforeSave(T entity, ValidationMode mode)` | Default-value population (e.g. auto-seq), screen-tran stamping (e.g. `"CSFD"` on insert). Runs before `validate`. |
| `onDuplicate(ID id)` | Customise the duplicate-key error message thrown by `insert`. |
| `search(T probe)` | Override only for SQL-backed filtering (e.g. `repository.findAll(spec)`). The default reflects over the probe's non-null bean properties and filters `findAll()` in-memory — fine at demo scale. |
| Add a screen-specific finder | Expose on the `XxxActivity` interface + impl it on the bean. Example: `TraderActivity.namesByCompany(companyCode)` for trader-name hydration. |

## The four worked examples in the repo

| Class | Workflow | What it shows |
|---|---|---|
| `ReportTextActivityService` | `reportText.maintenance` | Single-grid CRUD with no surprises — the baseline. Composite PK via `@IdClass`, no custom verbs. |
| `CorporateShipmentFlowActivityService` | `corporateShipmentFlows` | Master grid with a `cmd_details` custom verb that loads children server-side and returns a `componentType: "parentChild"` envelope (via `ProcessResponse.changePage`) so the engine navigates to CSFD with both header form + detail grid populated in one roundtrip. |
| `CorporateShipmentFlowDetailsActivityService` | `corporateShipmentFlowDetails.maintenance` | Detail-side of master/detail. Uses the base class's `parentValue` / `withMutatedData` / `asSingletonModel` / `refreshedChildren` helpers — each verb is one or two lines. |
| `TraderPromptActivityService` | `trader.prompt` | Read-only picker. Extends `ReadOnlyActivityService` so the four write verbs throw automatically; overrides `readOnlyError()` to customise the message. |
| `SysParametersActivityService` | `sysParameters.maintenance` | Singleton-edit (WSPM). Extends `AbstractEngineActivity` **directly** (sibling of the CRUD base — mirrors CSnx's `WaddActivityService_WSPM extends BaseActivityService`). Implements only `cmdSearch` (load-or-default the one row) and `cmdUpdate` (upsert); blocks `cmdCreate`/`cmdDelete`/`cmdCopy` with a friendly "edit only" message. The screen JSON's `action: "cmd_search"` field fires the load on screen mount. |

Read these in order when adding a fifth.

## Response factories on the base class

`AbstractCrudActivityService` exposes five response-shape factories so a
concrete activity never hand-builds a `ProcessResponse` or
`ProcessModelHolder`:

| Helper | Shape | When |
|---|---|---|
| `okGrid(command, envelopes)` | `componentType: "grid"`, `actionType: "none"` | cmd_search default, cmd_create echo (base class already uses this). |
| `okGridUpdate(command, envelopes)` | `componentType: "grid"`, `actionType: "update"` | cmd_update — drives `mergeRowsByKey` on the engine. Base class already uses this. |
| `okForm(command, envelope)` | `componentType: "form"`, singular `.model` | Master-detail cmd_create / cmd_copy via `EntityDialog`'s append branch. |
| `okParentChild(command, master, children)` | `componentType: "parentChild"`, master + children | Master-detail grid refresh, server-driven master+children loads. |
| `okEmptyGrid(command)` | empty grid | cmd_delete result on single-grid screens. |

For server-driven navigation (custom verbs like `cmd_details` that load
data and route the user to a different workflow), use the static
factory `ProcessResponse.changePage(targetWorkflow, command, modelHolders)`
— it sets `changePage: true` and rewrites the workflow on the response.

## Master-detail plumbing helpers

Generic helpers on the base class for the master-detail patterns
described in `engine.md` §20:

- **`parentValue(request, fieldName)`** — read a field from
  `request.parentModel`, falling back to `request.selectedModels[0]`.
  Use this everywhere the activity needs a parent-PK value; it handles
  the toolbar's `performDelete` / `performInlineUpdate` paths that ship
  requests *without* `parentModel`.
- **`withMutatedData(request, mutator)`** — return a copy of the
  request with the default holder's `model.data` mutated by the
  consumer. Use to inject parent context or strip identity PKs before
  delegating to the base class's verb.
- **`asSingletonModel(response)`** — reshape a `.models[0]` base-class
  response to a singular `.model` form response. Master-detail
  cmd_create / cmd_copy use this so EntityDialog's append branch can
  read the saved row.
- **`refreshedChildren(request, requeryChildren)`** — wrap a
  `Supplier<List<ProcessModelEnvelope>>` of re-queried children as a
  `parentChild` response. The base sets the master envelope from
  `request.parentModel`; the activity just provides the re-query
  lambda.
- **`toEnvelopes(rows)`** — map a list of entities through `toData` into
  envelopes. Saves a four-line loop at the call site.
- **`toDataAuto(entity)`** — reflection-discovered wire shape: walks
  the entity's class hierarchy collecting every readable, non-static
  declared field (cached per class). The instance `toData(T)` default
  delegates here; call the static helper directly when you need the
  wire map for an entity that isn't your activity's `T` (e.g. COSF's
  `cmd_details` ships `ShipmentFlowDetail` rows alongside its own
  master). Conceptually mirrors CSnx's
  `BaseEntity.getTypes(clazz, types)` →
  `FieldUtility.getAllTypes(clazz)` discovery.
- **`toDataWithFields(entity, fieldNames...)`** — explicit-list variant
  of `toDataAuto`. Use when you want to control the exact set / order
  of fields on the wire (e.g. omit sensitive columns, force a specific
  JSON ordering). `fromData` stays hand-written because it carries
  required-field validation, type coercion, and default values that
  reflection would obscure.

The canonical CSFD wiring built on these helpers is:

```java
@Override public ProcessResponse cmdCreate(ProcessRequest request) {
    return asSingletonModel(super.cmdCreate(withInsertContext(request)));
}
@Override public ProcessResponse cmdUpdate(ProcessRequest request) {
    return super.cmdUpdate(withParentInjected(request));
}
@Override public ProcessResponse cmdCopy(ProcessRequest request) {
    return asSingletonModel(super.cmdCopy(withInsertContext(request)));
}
@Override public ProcessResponse cmdDelete(ProcessRequest request) {
    super.cmdDelete(request);
    return refreshedChildren(request, () -> queryChildEnvelopes(request));
}
```

Where `withParentInjected` / `withInsertContext` are two-line activity-
private wrappers around `withMutatedData(request, data -> …)` that know
the activity's parent-PK field name.

## Audit-stamp contract

Entities that extend `MaintainableTranUserBaseEntity` automatically get:

- `MAINT_DATE` and `MAINT_TIME` stamped on every save via
  `@PrePersist` / `@PreUpdate` (declared on
  `MaintainableDateTimeBaseEntity` — see `dao-patterns.md`).
- `MAINT_USER` and `MAINT_TRAN` stamped by
  `AbstractCrudActivityService.applyAuditStamps` — `MAINT_USER` from the
  Spring Security principal, `MAINT_TRAN` from the verb constant
  (`INSERT` / `UPDATE` / `DELETE`).

Override `MAINT_TRAN` in `beforeInsert` if the screen wants a screen-id
trail instead of the verb (CSFD writes `"CSFD"`). The activity service
never sets `MAINT_DATE` / `MAINT_TIME` directly — that's the entity
superclass's job.

## Registering the service

Annotate the class `@Service` — Spring's component scan + the activity
registry (looked up by `workflow()` value) handles everything else. The
constructor takes the repository (typed to the same `T, ID`) plus any
collaborators (sibling repositories, lookup services).

## What lives where

- The activity service + base infra: `backend/src/main/java/za/co/csnx/demo/service/activity/`
- The activity bean (interface + impl) + base bean: `backend/src/main/java/za/co/csnx/demo/business/activity/`
- The matching screen JSON: `backend/src/main/resources/screens/<workflow>.json`
- The entity: `backend/src/main/java/za/co/csnx/demo/domain/`
- The repository: `backend/src/main/java/za/co/csnx/demo/repository/`
- Tests: `backend/src/test/java/za/co/csnx/demo/service/activity/` (one
  per activity, pattern: `@SpringBootTest` + Testcontainers; verify
  each verb plus any custom envelope reshaping).

## Porting from CSnx

When the activity exists in CSnx, the CSnx source is the conceptual
reference. Open it before writing the demo version:

- CSnx activities: `C:\software\projects\CSnx\src\za\co\csnx\csnx\server\services\activity\`
- CSnx business beans: `C:\software\projects\CSnx\src\za\co\csnx\csnx\business\`
- CSnx base class: `BaseCRUDActivityService` (the conceptual parent of
  demo's `AbstractCrudActivityService`).

Map verb-by-verb (`processCommand` switch in CSnx → one method per
verb in demo) and field-by-field (the CSnx entity's Java field names
carry over unchanged; only column annotations get lowercased). When
CSnx extracts a helper (parent-context injection, response-envelope
reshape, etc.), keep the helper in demo with a similar name and shape
so the next port follows the same playbook. Modernise the plumbing —
Hibernate, MapStruct, Spring DI, `record` DTOs — but not the
abstraction shape.

**When a ported screen misbehaves, suspect the backend first.** Demo's
frontend engine was lifted from csnx-ui and is exercised by every
working screen plus the Playwright suite. If the equivalent screen in
csnx-ui works against its own backend, the engine code path is sound —
the bug is almost always in demo's activity service, response-envelope
shape, or screen JSON. Reproduce against the live stack
(`http://localhost:5173/`), compare the `/api/process` request +
response to the csnx-ui equivalent, and fix the backend mismatch.
Reach for engine-side changes only after ruling out the backend; the
CSFD "delete didn't refresh grid" landmine (toolbar ships cmd_delete
without `parentModel`; activity must fall back to selectedModels) is
the canonical example of "looks like a frontend bug, was actually
backend".

## Related

- **`engine.md`** §10 (process pipeline) and §16 (activity contract) own
  the wire shapes this guide builds on.
- **`engine.md`** §20 lists the master-detail landmines the helpers
  above were written to navigate.
- **`dao-patterns.md`** for the entity / repository conventions.
