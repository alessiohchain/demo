# Engine specification

The metadata-driven UI engine: workflows expressed as field / button / toolbar
metadata, served by Spring Boot from `/api/metadata`, executed by activity
services via `/api/process`, and rendered by a React runtime in
`frontend/src/engine/`.

This file is the **specification** — what the engine guarantees, the wire
shape of every request and response, how a button click flows through the
system, and what the back-end has to honour for a new screen to light up.
`CLAUDE.md` is the conventions guide; `docs/architecture.md` is the broader
backend/frontend wiring; this file is the engine contract.

---

## 1. Purpose & non-goals

**The engine renders any workflow from declarative metadata** — no
hand-coded React per screen. Adding a new screen is a back-end exercise
(write an activity service + seed metadata + grant a fastpath) followed by
a smoke test. The front-end is unchanged.

In scope:

- Form / Grid / Master-Detail / Search / Filter / Toolbar screens.
- CRUD verbs (`cmd_search`, `cmd_create`, `cmd_update`, `cmd_delete`,
  `cmd_copy`, `cmd_filter`) plus custom activity verbs.
- Field-type rendering (text, number, date, time, boolean, select).
- Inline-edit grids with touched-only validation.
- Server-driven dialogs (Entity, Confirm, MessagePrompt, Reauth, Upload).
- Lookup data and fastpath/menu routing.

Out of scope (for now):

- Charting / report rendering — the metadata schema has slots but the
  renderers are not wired.
- File upload via `cmd_upload` — `UploadDialog` exists, the wire shape
  is defined, but no demo workflow exercises it yet.
- Advanced filter UI from `FilterMetadata` — wire shape round-trips
  correctly but no filter renderer is wired (filters work via the
  `filterDataHolder` payload assembled by the toolbar code).

---

## 2. Runtime data flow

```
URL  ── React Router ──> DynamicScreen
                              │
                              ├─ GET /api/metadata?workflow=…
                              │       returns MetadataHolder
                              │       { name, shortName, metadataList[] }
                              │       — one-shot per workflow per cache version
                              │
                              ├─ POST /api/process
                              │       { workflow, command, modelHolders, … }
                              │       — every command click. Returns ProcessResponse.
                              │
                              └─ renders <Toolbar/> <DynamicForm/> <DynamicGrid/>
                                  wrapped in <ScreenProvider />
```

Boot sequence:

1. After login, the SPA fetches `GET /api/lookup/init` once. The payload
   carries the `(company, fastpath → workflow, menuTree, vvd
   dictionaries)` bundle that drives the sidebar, fastpath input, and
   every dropdown field on every subsequent screen.
2. The user navigates to a fastpath (sidebar click or `FastpathInput`).
3. `DynamicScreen` resolves the fastpath → workflow id from the lookup
   bundle, calls `GET /api/metadata?workflow=…`, and renders the
   returned `metadataList`.
4. Toolbar clicks call `POST /api/process` with the modelType-derived
   payload. Responses route through `useProcessResponseHandler` which
   applies modelHolder updates, surfaces exceptions, navigates, opens
   downloads, etc.

Class anchors:

| Concern              | Front-end (`frontend/src/`)                   | Back-end (`backend/src/main/java/za/co/csnx/demo/`)            |
|----------------------|-----------------------------------------------|----------------------------------------------------------------|
| Fastpath → workflow  | `app/router.tsx`, `shell/FastpathInput.tsx`   | `service/LookupService.java`                                   |
| Metadata fetch       | `engine/screen/DynamicScreen.tsx`             | `web/MetadataController.java`                                  |
| Process dispatch     | `engine/process/useProcess.ts`                | `web/ProcessController.java` → `engine/ActivityRegistry.java` → `engine/ActivityService.java` |
| Activity contract    | n/a                                           | `engine/AbstractCrudActivityService.java`, `engine/ActivityService.java` |
| Form render          | `engine/form/DynamicForm.tsx`                 | DTOs in `web/dto/engine/`                                      |
| Grid render          | `engine/grid/DynamicGrid.tsx`                 | DTOs in `web/dto/engine/`                                      |
| Toolbar dispatch     | `engine/toolbar/Toolbar.tsx`, `buttonPayload.ts` | n/a — back-end is reactive to the verb              |
| Lookup bootstrap     | `engine/lookup/useLookup.ts`                  | `web/LookupController.java`                                    |
| Screen state         | `engine/screen/ScreenContext.tsx`             | n/a                                                            |
| Response handling    | `engine/process/useProcessResponseHandler.ts` | n/a                                                            |

---

## 3. Back-end surface

Three engine endpoints, all under `/api`. All require a JWT bearer token
except where noted. Identity comes from `SecurityContextHolder` (via
`JwtAuthFilter`) — never from the request body.

### `GET /api/metadata`

Returns the workflow's `MetadataHolder`.

Query parameters:

| Name        | Required | Notes |
|-------------|----------|-------|
| `workflow`  | yes      | Workflow id (e.g. `reportText.maintenance`). |
| `language`  | no       | ISO language; defaults to the user's language. |
| `fastpath`  | no       | 4-letter fastpath code. Used for right-grant resolution when the same workflow is reachable from multiple fastpaths. |

Response: `200 OK` with `MetadataHolder`. Resolution is driven by the
`screen_metadata` table — see §4.

### `POST /api/process`

Executes a workflow command. Request body is `ProcessRequest` (see §4
for fields); response is `ProcessResponse`.

**Failure convention:** the engine treats `exception != null` on the
response as **failure regardless of HTTP status**. `BusinessException`
inside an activity becomes a 200 with the exception envelope populated
— the front-end surfaces it as a toast / message dialog. Genuine
5xx-class failures escape to `GlobalExceptionHandler`.

### `GET /api/lookup/init`

Returns the post-login lookup bundle:

```json
{
  "data": {
    "Company":   { "WCS": "WCS Demo Company" },
    "Language":  { "EN": "English" },
    "WarehouseFacility": { "WCS-MAIN / WCS-WH1": "Main facility, Warehouse 1", ... },
    ...
  },
  "version": 17,
  "userDefaults": { "facility": "WCS-MAIN", "warehouse": "WCS-WH1" },
  "fastpathToWorkflow": { "RPTM": "reportText.maintenance", ... },
  "menuTree": [ ... ]
}
```

The client caches the response and reuses it for the session. Refetches
fire when:

- A `ProcessResponse` carries a higher `cachVersion.lookupData`.
- A button declares `clearCacheType: "LOOKUP"`.
- The user switches warehouse / facility.

(Login itself returns `userDefaults`, `fastpathToWorkflow`, and the
menu tree on the `AuthResponse` envelope so the shell can render
immediately without waiting for the bundle fetch.)

---

## 4. Metadata schema

Wire types are declared on both sides:

- TypeScript: `frontend/src/engine/types.ts`.
- Java DTOs: `backend/src/main/java/za/co/csnx/demo/web/dto/engine/`.

### `MetadataHolder` — top-level wrapper

| Field              | Meaning                                                           |
|--------------------|-------------------------------------------------------------------|
| `name`             | Workflow id.                                                      |
| `shortName`        | 4-letter fastpath code (used by breadcrumb if no menu entry).     |
| `language`         | ISO language the metadata was resolved for.                       |
| `version`          | Cache-bust counter — `ProcessResponse.cachVersion.metadata` bumps this. |
| `autoRefreshTime`  | Polling interval for screens that opt into auto-refresh (ms).     |
| `action`           | Default action verb on first load (e.g. `enable_edit`).           |
| `metadataList`     | Ordered list of `FormMetadata` / `GridMetadata` / `SearchMetadata` / `FilterMetadata` / `ToolbarMetadata`. |

### Screen types

| Screen type      | `metadataList` contents                              |
|------------------|------------------------------------------------------|
| Form             | One `FormMetadata` (`kind: "form"`)                  |
| Grid             | One `GridMetadata` (`kind: "grid"`)                  |
| Master-Detail    | Form followed by Grid                                |
| Search           | One `SearchMetadata` (`kind: "search"`) wrapping a form + grid |
| Filter           | One `FilterMetadata` (`kind: "filter"`)              |
| Toolbar-only     | One `ToolbarMetadata` (`kind: "toolbar"`)            |

### `FieldMetadata` — one form field or grid column

Important fields:

| Field                | Meaning |
|----------------------|---------|
| `name`               | Stable field key. Must match the back-end model property name. |
| `label`              | Display label. |
| `type`               | Java-type string — see [Field input types](#5-field-input-types). |
| `uiType`             | Render-time override (`password`, `textarea`, `select`, `date`, `hidden`). |
| `format`             | Format hint (date pattern, number format). |
| `lookupDataKey`      | When set, field is a select — names a key in the lookup-data map. |
| `defaultValue`       | Initial value when the form mounts in create mode. |
| `nullable`           | When false, validation fails on empty input. |
| `validators`         | Named validators applied in addition to type / length checks. |
| `min` / `max`        | Range bounds. |
| `length` / `maxlength` | Display width / max characters. |
| `decimals`           | Decimal places for numeric fields. |
| `regexExpression`    | Validation regex + `regexErrorMessage`. |
| `multiSelect`        | Multi-value selection. |
| `align`              | `left` / `right` / `center` cell alignment. |
| `sectionName`        | Field group (drives collapsible sections on forms). |
| `summaryFunctionName`| Grid column footer aggregate (`sum`, `count`, `count_distinct`, …). |
| `relatedFields`      | Sibling fields used by composite summaries. |
| `clickForDetail`     | Grid cell click fires `cmd_detail` on the row. |
| `emptyRow`           | When false, dropdown has no leading blank — user must pick. |
| `focus`              | Auto-focus on form mount. |
| `linkedButton`       | Enter-in-field fires the named button. |
| `fixedValue`         | Read-only baked-in value (engine constants). |
| `styleValueMap`      | `value → css string` for cell colouring. |
| `keyPressFilter`     | Regex applied per-keystroke. |
| `stateVisibleMap` / `stateEditableMap` | Per-screen-state cascade. See §7. |
| `right`              | Per-field right code — `"N"` hides, `"R"` disables. |
| `tooltip`            | Hover text. |
| `menuItems`          | Field-level info menu (jump to other fastpaths carrying the value). |
| `linkItem`           | Field-level prompt button. |
| `viewFieldMetadata`  | Grid display-only renderer when view differs from edit. |

### `ButtonMetadata` — one toolbar button

| Field                | Meaning |
|----------------------|---------|
| `name`               | Stable button key. Matches `data-button-name` in the DOM. |
| `value`              | Display text. |
| `label`              | Fallback display text for custom buttons. |
| `command`            | Verb sent on the `/api/process` request. |
| `accessKey`          | Keyboard shortcut letter. |
| `tooltip`            | Hover text. |
| `right`              | Role-grant right code — `"N"` hides, `"R"` disables. |
| `local`              | `true` → handle client-side (no server roundtrip). |
| `validate`           | Default `false`. See §7. |
| `forwardTo`          | Workflow id — clicking navigates instead of calling the server. |
| `modelType`          | Drives request payload shape. See §6. |
| `allData`            | Ship every grid's full row set on top of modelType's bundle. |
| `authenticate`       | Prompt re-auth dialog before dispatching. |
| `parentData`         | Include parent form's values as `parentModel`. |
| `returnErrorCodes`   | Echo prior-response error codes back. |
| `previousModel`      | Include the previously-loaded snapshot. |
| `clearCacheType`     | `LOOKUP` / `METADATA` / `MENU` — invalidate after success. |
| `excludeFields`      | Fields stripped from seeded values in Copy popups. |
| `defaultFields`      | Fields auto-populated in Add / Copy popups. |
| `stateVisibleMap` / `stateEditableMap` | Per-screen-state cascade for the button itself. |

### `FormMetadata`, `GridMetadata`, `ToolbarMetadata`, `FilterMetadata`

| Type                | Carries                                                          |
|---------------------|------------------------------------------------------------------|
| `FormMetadata`      | `fieldList[]`, `toolbarMetadata?`, `columns`, `header`, `footer`, `mainSectionName`, form-level `stateVisibleMap`/`stateEditableMap`. |
| `GridMetadata`      | `fieldList[]` (columns), `toolbarMetadata?`, `searchMetadata?`, `checkboxSelection`, `inlineEdit` (default `true`), `preferences`. |
| `ToolbarMetadata`   | `actionButtons[]`, `navigationButtons[]`, `large` (size flag).   |
| `FilterMetadata`    | `fieldList[]`, `toolbarMetadata?`.                               |

### Metadata storage

Demo stores screen metadata in the `screen_metadata` table (`payload`
JSONB column per workflow). `MetadataController` queries it via
`ScreenMetadataRepository` and returns it as the `MetadataHolder`.

**Source of truth for the payload is a JSON file under
`backend/src/main/resources/screens/<workflow>.json`.** At every boot,
`ScreenMetadataSeeder` (`service/ScreenMetadataSeeder.java`) scans
that directory, deserialises each file into `MetadataHolder`
(structural validation — boot fails fast if a file is malformed),
hashes the canonicalised payload, and upserts the row only if the
hash differs from `screen_metadata.payload_hash`. Hibernate's
`@Version` (`update_serial`) bumps on update, driving the engine's
cache-bust on the front-end.

Authoring a payload change is therefore "edit the JSON file, restart"
— no Flyway migration needed. Flyway migrations remain the home of
one-time enablement (menu placement, role grants) and schema
changes.

`reportText.maintenance.json` references the local `_schema.json`
draft-07 JSON Schema at the top of the file. Open either file in
VS Code or IntelliJ for autocomplete + structural validation while
authoring.

---

## 5. Field input types

The back-end stamps a Java-type string into `FieldMetadata.type`. The
renderer maps it to a TypeScript type for the form state:

| Wire string             | TypeScript shape  |
|-------------------------|-------------------|
| `java.lang.String`      | `string`          |
| `java.lang.Integer`     | `number`          |
| `java.lang.Long`        | `number`          |
| `java.lang.Double`      | `number`          |
| `java.math.BigDecimal`  | `number`          |
| `java.lang.Boolean`     | `boolean`         |
| `java.util.Date`        | `Date`            |
| `java.sql.Date`         | `Date`            |
| `java.sql.Time`         | `Date`            |
| `java.sql.Timestamp`    | `Date`            |

Values outside this set fall through to `string` rendering — the
back-end is responsible for canonicalising before shipping. Inputs are
coerced back to typed wire shapes on submit using `fieldTypes` from
`ScreenContext`.

---

## 6. modelType routing

When a non-local button fires, `engine/toolbar/buttonPayload.ts`
assembles the `/api/process` request body from the button's `modelType`
plus the screen's current form values, grid edits, and selection state.

| `modelType`                  | Ships form? | Ships grid?    | Selection gate         | Validation when `validate=true`         |
|------------------------------|-------------|----------------|------------------------|------------------------------------------|
| `none`                       | no          | no             | none                   | none                                     |
| `forms`                      | yes         | no             | none                   | form                                     |
| `grids`                      | no          | yes (all rows) | none                   | rows (edited only)                       |
| `entire_grid`                | no          | yes (all rows) | none                   | rows (edited only)                       |
| `entire_selected_grid`       | no          | yes (selected) | ≥1 selected            | rows (edited only)                       |
| `selected`                   | no          | yes (selected) | ≥1 selected            | rows (edited only)                       |
| `single`                     | no          | yes (1 row)    | exactly 1 selected     | row (edited only)                        |
| `all`                        | yes         | yes (all)      | none                   | form + rows                              |
| `form_selected`              | yes         | yes (selected) | ≥1 selected            | **form always** + rows (when `validate`) |
| `form_entire_selected_grid`  | yes         | yes (selected) | ≥1 selected            | **form always** + rows (when `validate`) |
| `form_entire_grid`           | yes         | yes (all rows) | none                   | **form always** + rows (when `validate`) |
| `form_single`                | yes         | yes (1 row)    | exactly 1 selected     | **form always** + row (when `validate`)  |

Defaults: a button with no explicit `modelType` is treated as
`selected`.

Helpers in `engine/toolbar/buttonPayload.ts`:

- `modelTypeIncludesForm(button)` — true for `all` / `forms` / `form_*`.
- `modelTypeIncludesGrid(button)` — true for any grid-bearing modelType.
- `shouldValidate(button)` — `button.validate === true`.

Selection-gate violations surface as toasts: `NO_ROWS_SELECTED` /
`TOO_MANY_ROWS_SELECTED`.

---

## 7. Validation behaviour

### `validate` defaults to `false`

A button with no explicit `validate` attribute **skips client-side
validation** — the click routes straight to the back-end and the
activity enforces constraints. `shouldValidate(button)` is the single
source of truth: `button.validate === true`.

### `form_*` modelTypes always validate the master form

Every modelType prefixed `form_` **always** validates the master form
regardless of the `validate` flag. The `validate` flag still gates row
validation in the grid portion of the same call.

Master-form validation **never** runs for grid-only modelTypes. Gate via
`modelTypeIncludesForm(button)` before any form validation call.

### Visible / editable cascade

`engine/form/validation.ts` (`validateFields`) skips fields the user
can't see or edit. Resolution order:

1. `right` — role-grant right code. `"N"` → invisible, `"R"` → disabled.
2. `stateEditableMap[currentState]` / `stateVisibleMap[currentState]` —
   per-field override for the active screen state. Form-level overrides
   cascade in if the field has none of its own.
3. `defaultEditableState` / `defaultVisibleState` — fallback.

Required fields the user can't see or edit in the current screen state
never block a Save.

### Grid inline-edit validation

Inline-edit row validation checks **only user-touched fields of edited
rows**. Pristine rows are trusted, untouched fields in an edited row are
trusted. This lets bulk inline-edit flows update one field across many
rows without re-validating every untouched cell.

### Navigation commands bypass validation

Buttons whose `command` is one of:

```
cmd_close, cmd_back, cmd_cancel, cmd_skip, cmd_next, cmd_prev, cmd_previous
```

skip pre-submit validation entirely. The conventional way to declare
them is `local=true`. If a screen needs a navigation command that still
hits the server, keep `local=false` and add `validate=false`.

---

## 8. Process pipeline

### Request — `ProcessRequest`

Source: `backend/src/main/java/za/co/csnx/demo/web/dto/engine/ProcessRequest.java`.

| Field                  | Notes |
|------------------------|-------|
| `workflow`             | Workflow id. Required. |
| `command`              | Verb. Required. |
| `modelHolders`         | `Map<componentName, ProcessModelHolder>` — bundled forms / grid rows per the button's modelType. |
| `selectedModels`       | Selected grid rows (for `selected` / `single`). |
| `parentModel`          | Parent form values for master-detail popups. |
| `filterDataHolder`     | Advanced filter map. |
| `startIndex` / `rows`  | Paging. |
| `sortField` / `sortDirection` | Grid sort. |
| `componentKey`         | Specific component scope for the call. |
| `state`                | Current screen state. |
| `previousWorkflow` / `previousFasthPath` | Back-navigation context. |
| `clearCacheType`       | Pre-call cache invalidation. |
| `returnedErrorCodes`   | Echo of prior error codes (for escalation). |
| `previousModel`        | Snapshot for optimistic-locking. |
| `messageHolder`        | Round-tripped server messages. |
| `workflowOverride` / `workflowModels` | Workflow override (multi-workflow flows). |
| `fileName`             | File-bound commands. |
| `editableFieldNames` / `visbileFieldNames` | Per-call field-state overrides. |

### Response — `ProcessResponse`

Source: `backend/src/main/java/za/co/csnx/demo/web/dto/engine/ProcessResponse.java`.

The engine treats `exception != null` as **failure regardless of HTTP
status**. Genuine 5xx errors escape to `GlobalExceptionHandler`.

| Field                  | Notes |
|------------------------|-------|
| `modelHolders`         | Returned model data — populates `componentData` in `ScreenContext`. |
| `selectedModels`       | Updated selection. |
| `exception`            | `ExceptionEnvelope` — business / system error. |
| `messageHolder`        | Info / warning / prompt messages. |
| `changePage`           | Triggers screen navigation. |
| `openURL`              | Browser navigates / downloads. |
| `fileName` / `directoryName` / `deleteFile` | File-streaming details. |
| `clearCacheType`       | Triggers TanStack Query invalidation. |
| `cachVersion { metadata, lookupData }` | Bumps trigger metadata / lookup refetch. |
| `state`                | Screen state override. |
| `fieldMetadataMap` / `buttonMetadataMap` | Per-call attribute overrides. |
| `focusField`           | Pulse focus after the response is applied. |
| `keep` / `skip` / `updateParent` / `updateNavigation` | Master-detail return-trip hints. |
| `abort`                | Abort the dispatched command without applying any model updates. |
| `enableBreadCrumbNavigation` | Toggle breadcrumb history-pop behaviour. |
| `initComponents`       | Force a full re-init of every component on the screen. |

### Dispatcher — `useProcessResponseHandler`

Single point of interpretation for every response. Behavioural rules:

- `exception != null` → surface as toast / multi-error dialog; do **not**
  apply `modelHolders`.
- `messageHolder.prompt === true` → block in `MessagePromptDialog` until
  user resolves.
- `changePage` → router navigate.
- `openURL` → browser navigate or download.
- `skip + updateParent` → refresh parent grid cache, then `navigate(-1)`.
- `cachVersion.metadata > current` → invalidate metadata cache for the
  workflow.
- `cachVersion.lookupData > current` → invalidate lookup-data cache.
- Always: invalidate `['grid-search', workflow]` after a successful
  mutation so the grid refetches.

### Cache invalidation pattern

EntityDialog (Create / Update / Copy / Search) and Toolbar mutations
(`cmd_delete`, inline-edit Update, master-detail Save) invalidate
`['grid-search', workflow]` after a successful response. TanStack
Query's prefix invalidation matches every filter combination of the
active query — the grid refetches with the user's current filter,
sort, and paging intact.

---

## 9. Activity service contract

Back-end activities implement the `ActivityService` interface and
register with `ActivityRegistry`. Most CRUD screens extend
`AbstractCrudActivityService<T, ID>` which provides default
implementations of every standard verb.

Source: `backend/src/main/java/za/co/csnx/demo/engine/`.

```
ActivityService                (interface — workflow + process(request))
        ▲
        │
AbstractCrudActivityService    (template — cmdSearch/Create/Update/Delete/Copy/Filter)
        ▲
        │
<your activity>                (override searchAll, idFromData, toData, fromData, applyCompanyScope, …)
```

Standard verbs:

| Method                 | Default behaviour |
|------------------------|-------------------|
| `cmdSearch(request)`   | Calls `searchAll(companyCode, request)` and wraps each row via `toData(entity)`. |
| `cmdCreate(request)`   | Reconstructs PK via `idFromData`, calls `validateDuplicate`, creates entity via `newEntity()` + `applyCompanyScope` + `fromData`, stamps `applyAuditStamps`, saves. |
| `cmdUpdate(request)`   | Reconstructs PK, loads existing, calls `beforeUpdate(existing, data)`, applies `fromData`, stamps, saves. |
| `cmdDelete(request)`   | Loads each `selectedModel`, deletes. Errors if zero selected. |
| `cmdCopy(request)`     | Same as `cmdCreate`. |
| `cmdFilter(request)`   | Flattens `filterDataHolder` into a criteria map, delegates to `cmdSearch`. |

Required subclass hooks:

- `workflow()` — return the workflow id.
- `newEntity()` — factory.
- `idFromData(data, companyCode)` — reconstruct composite PK.
- `fromData(data, entity)` — map wire data → entity.
- `toData(entity)` — map entity → wire data.
- `applyCompanyScope(entity, companyCode)` — stamp company on insert
  (default no-op).
- `validateDuplicate(id)` — guard against insert collisions (default
  uses `existsById`).
- `searchAll(companyCode, request)` — query filter override.

Identity helpers (inherited):

- `currentCompanyCode()` — parses `WCS|wcs` from the JWT subject.
  Throws `BusinessException` if no `|` separator (i.e. user
  authenticated without a company prefix).
- `currentUsername()` — returns the part after the `|`.

Custom verbs: override `cmdCustom(request)` to dispatch unknown
commands by name.

---

## 10. Toolbar dispatch

`engine/toolbar/Toolbar.tsx` renders the button row and dispatches
clicks. Dispatch order:

1. `authenticate=true` → `ReauthDialog`. Re-auth flow returns to the
   originating button after success.
2. `forwardTo` → router navigate. No server call.
3. `local=true` → `handleLocal` — open `EntityDialog`, advance the
   detail queue, or run client-only verbs.
4. Else → server call. Payload assembled by `buildButtonPayload` per
   the modelType table in §6.

Pre-submit validation runs through `validateBeforeServerCall`, which
gates only when the modelType ships the master form.

In-flight state is tracked in `engine/toolbar/buttonState.ts` — buttons
render with a spinner and ignore further clicks until the response
arrives.

---

## 11. Form rendering

`engine/form/DynamicForm.tsx` is the renderer. Field components live
under `engine/form/fields/`.

- Forms are **uncontrolled** — keyed on `initialValues.updatedAt` so a
  server-driven reload remounts with fresh values. Inputs read from the
  DOM, not React state.
- `FieldRenderer` dispatches to a per-field-type component based on
  `type` + `uiType`.
- `FieldWithActions` wraps each field with its info-menu / prompt-button
  / cell-link actions.
- `Collapsible` wraps a `sectionName` group into an expand/collapse
  section.
- `validation.ts` (`validateFields`) is the canonical validator (see §7).

Form values live in DOM. On submit, the toolbar reads them via
`buildButtonPayload` and coerces them using `fieldTypes` from
`ScreenContext`.

---

## 12. Grid rendering

`engine/grid/DynamicGrid.tsx` is TanStack Table-backed.

Features:

- Inline-edit cells (click to edit). `editedRows` lives in
  `ScreenContext`.
- Row selection via leading checkbox column when `checkboxSelection ===
  true`.
- Column reorder / resize / hide via headers. Layout persists per
  `(workflow, gridName)` in `localStorage` (`gridPrefsStore.ts`).
- Sticky summary footer when any column has `summaryFunctionName`.
  Aggregator dispatch is in `summaryRegistry.ts`.
- Per-column cell styling via `cellStyleRegistry.ts` (extension hook for
  screen-specific colouring on top of `styleValueMap`).
- CSV export via `exportCsv.ts`.

Inline-edit validation: touched-only (see §7).

---

## 13. Screen context

`engine/screen/ScreenContext.tsx` holds per-screen shared state:

| Slot                | What it carries |
|---------------------|-----------------|
| `componentData`     | `Map<componentName, model data>` — materialised `modelHolders` from the latest response. |
| `selectedRows`      | Per-grid `Set<rowKey>` of selected rows. |
| `editedRows`        | Per-grid `Map<rowKey, FieldName → value>` of inline edits. |
| `rowErrors`         | Per-grid `Map<rowKey, FieldName → errorMessage>`. |
| `formErrors`        | `Map<FieldName → errorMessage>` from form validation. |
| `filters`           | Active grid filters. |
| `fieldTypes`        | `Map<FieldName → Java type string>` for re-typing on submit. |
| `masterDetailBridge`| Master form → detail grid wiring. |

`ScreenProvider` is mounted by `DynamicScreen` per fastpath route —
leaving disposes it. `engine/screen/screenFlow.ts` provides cross-screen
cache cleanup utilities (used on sign-out).

---

## 14. Dialogs

`engine/dialog/` holds the dialog vocabulary:

| Dialog              | Used for |
|---------------------|----------|
| `EntityDialog`      | Create / Update / Copy / Search popups. Renders the appropriate metadata in form layout, fires the verb on Save, runs `useProcessResponseHandler`, invalidates `['grid-search', workflow]` after success. |
| `ConfirmDialog`     | Destructive-action confirmation. Standard for `cmd_delete`. |
| `MessagePromptDialog` | Server-driven blocking prompts (`messageHolder.prompt === true`). |
| `ReauthDialog`      | Buttons with `authenticate=true`. |
| `UploadDialog`      | File upload for `cmd_upload`. |
| `Modal`             | Base Radix-Dialog wrapper. Title rendered as `<h2 id="modal-title">`. |

`DialogContext.ts` tracks the active button + open dialog;
`entityDialogStore.ts` snapshots dialog values across prompt
round-trips.

---

## 15. Lookup bootstrap

`engine/lookup/useLookup.ts` populates a `LookupProvider` from the
`/api/lookup/init` response (§3). Every dropdown field reads its
values from the provider — no per-field XHR.

A pre-auth variant of the endpoint is available for the login screen
(company / language dropdowns before the user is authenticated).

The cache `version` from the response is stored in `cacheVersionStore`
and consulted on every `ProcessResponse` — bumps trigger a refetch.

---

## 16. Fastpath / menu / role-grant model

A 4-letter fastpath code is the user-facing entry point to a workflow.

| Concept       | What the engine depends on |
|---------------|----------------------------|
| **Fastpath**  | A 4-letter code that resolves to a workflow id + initial action verb. Carried by `fastpathToWorkflow` in the lookup bundle. |
| **Menu**      | Tree of `menu_item` rows placing fastpaths under sections. The sidebar renders this tree. |
| **Role grant** | A `(company, role, fastpath)` tuple that grants the current user access. Missing → the menu hides the entry and `/api/metadata` returns a 403 / "no metadata" business error. |

Storage:

- `screen_metadata` — workflow metadata JSON.
- `menu_item` — menu tree.
- Role / fastpath grant tables — TBD per module; demo currently uses
  a single ADMIN role granted to the seeded user.

Adding a new fastpath = Flyway migration inserting:
1. A row into `screen_metadata` with the workflow's JSON.
2. A row into `menu_item` placing it in the sidebar.
3. A role grant row for the company / role that should access it.

---

## 17. Selectors / test hooks

| Element              | Selector |
|----------------------|----------|
| Toolbar button       | `button[data-button-name="<command>"]` |
| Form field input     | `[data-field-name="<fieldName>"]` |
| Grid row             | `tbody > tr` (first `<td>` is selection checkbox) |
| Dialog title         | `h2#modal-title` |
| Multi-error dialog title | `h2#multi-error-title` |
| Network busy stripe  | `#network-activity-bar` |

Always select by field name (the `name` attribute) — labels can change
with locale; `name` is stable.

---

## 18. Adding a new screen

A new metadata-driven screen lights up automatically once the back-end
seeds and serves its metadata. Recipe:

1. **Activity service.** Subclass `AbstractCrudActivityService<T, ID>`
   for a CRUD screen, or implement `ActivityService` for a custom
   workflow. Register with `ActivityRegistry` (Spring component scan
   does this automatically when the bean is annotated). Override the
   required hooks (`newEntity`, `idFromData`, `toData`, `fromData`,
   `applyCompanyScope`, `searchAll`).
2. **Domain + repository.** Standard JPA — entity extends `BaseEntity`,
   repository extends `BaseRepository<T, ID>`.
3. **Metadata payload.** Create
   `backend/src/main/resources/screens/<workflow>.json`. Start by
   copying an existing screen file (e.g. `reportText.maintenance.json`)
   and reshaping. Keep the `"$schema": "./_schema.json"` line at the
   top for IDE help. Restart the backend; `ScreenMetadataSeeder`
   upserts the row into `screen_metadata` and bumps the cache version.
   No Flyway migration is needed for payload-only changes.
4. **Enablement (only for a brand-new fastpath).** Write one Flyway
   migration that inserts:
   - A `menu_item` row placing the fastpath under a section.
   - A role-grant row for the `(company, role, fastpath)` combination
     that should access it.
   The `screen_metadata.fastpath` column itself is populated from the
   JSON file's `shortName` field by the seeder — no SQL needed.
5. **Smoke test.** Launch the compose stack, log in, type the fastpath
   in the header, confirm:
   - The grid renders.
   - `cmd_search` fires correctly.
   - Add / Update / Delete round-trip with the activity.
6. **Watch the boot logs** — `ScreenMetadataSeeder` emits one line per
   seeded / updated / skipped file, plus a `warn` line for any
   workflow row that has no backing resource file. Drift surfaces
   here.

For incremental reshapes (rename a label, add a button, change a
validator), it's just step 3 — edit the JSON file, restart.

Bespoke (non-metadata) screens take a different path — see CLAUDE.md
§Conventions — frontend.

---

## 19. Wire contract guarantees

- `MetadataHolder` shape is stable. Activities adding new
  `FieldMetadata` / `ButtonMetadata` attributes don't require a front-end
  deploy.
- `ProcessRequest` / `ProcessResponse` round-trip opaquely. Activities
  adding new response fields don't require client changes unless the
  engine itself needs to interpret them.
- `exception != null` on a 200 response means business failure. Clients
  must not check HTTP status alone.
- `cachVersion` bumps are the canonical cue for the client to refetch
  metadata/lookup. Activities flushing the server-side cache must bump
  the version for the React invalidation to fire.
- All engine endpoints require JWT bearer auth except `/api/lookup/init`
  (pre-auth variant available for login).

---

## 20. Master-detail + picker pitfalls (learned the hard way)

These are landmines discovered building COSF/CSFD + the trader picker.
Each one cost time to diagnose; capturing them here so the next port
doesn't re-discover them.

### 20.1 `workflowOverride` is sticky — don't set it on a navigation response

`ProcessResponse.workflowOverride` is replayed on **every subsequent**
`/api/process` request by the axios interceptor (per `client.ts`'s
request interceptor). Setting it inside a `cmd_details` response (or any
navigation response) silently rewrites the workflow on every later call
from any screen — including a picker — until something explicitly clears
it.

For master→detail navigation, set `changePage: true` + populate the
response's `workflow` field, but leave `workflowOverride: null`.
`workflowOverride` is for activities that genuinely want a persistent
override across the session (rare).

### 20.2 Master-detail screen structure

For a master-detail screen (header context + detail grid + Add/Update
popup), the metadata `metadataList` needs **three** items in this order:

1. `kind: "form"` — the header form. Name it `<workflow>.header` (or
   any name distinct from the workflow). `defaultEditableState: false`
   on the form and every field — it's display-only context.
2. `kind: "form"` — the **popup** form for Add / Update. Name it
   `<workflow>.detail` (or similar). This form is **not rendered
   inline** — the engine's `shouldRenderInline` filters it out because a
   sibling grid references it via `searchMetadata.name`. It has the
   fields the operator edits (Trader, Type, times — NOT the parent
   context fields).
3. `kind: "grid"` — the detail grid. Name it after the workflow (e.g.
   `<workflow>`). Crucially, set `searchMetadata.name` to the popup
   form's name (item 2). That's what tells the engine "this grid's
   Add/Update popup uses that form". `findPopupForm` in
   `DynamicScreen.tsx` does the lookup; without it the engine falls
   back to the first inline form on the screen — which would be the
   read-only header — and the Add dialog shows the wrong fields.

Two top-level items (form + grid only) renders, but
`shouldRenderInline` then fires `cmd_search` from the wrong component
key and the response models land in a slot the grid doesn't read.

### 20.3 Composite PK + DB-generated identity → use `@SequenceGenerator`

`@IdClass`-composite-PK entities **cannot** use
`@GeneratedValue(strategy = IDENTITY)` on any of their `@Id` columns —
Hibernate rejects it with "Identity generation isn't supported for
composite ids".

Use `@SequenceGenerator` referencing the underlying Postgres sequence
instead:

```java
@Id
@GeneratedValue(strategy = SEQUENCE, generator = "shipment_flow_detail_seq")
@SequenceGenerator(
    name = "shipment_flow_detail_seq",
    sequenceName = "demo.shipment_flow_detail_shipment_flow_id_seq",
    allocationSize = 1)
@Column(name = "shipment_flow_id", nullable = false)
private Long shipmentFlowId;
```

The DDL still declares the column as `BIGSERIAL`; Postgres exposes its
implicit sequence by the conventional name
`<table>_<column>_seq` which JPA can target directly.

### 20.4 `cmd_details` should return a `parentChild` envelope, not just the master

The master-detail bridge in the front-end (`useProcessResponseHandler`
line ~460) recognises three shapes for a navigation response that
should populate both a parent form and a child grid on arrival:

1. **Nested holders** — `modelHolders[""].modelHolders[0]` with
   `{model: master, models: [children]}`.
2. **Flat parentChild** — `modelHolders[""] = { model: master,
   models: [children], componentType: "parentChild" }`.  ← we use this
3. **Detail iteration** — flat `models[]` only, no master.

Returning just the master (option 1 without the children) leaves the
detail grid empty on arrival; the user has to click Search manually.
Returning the parentChild envelope (option 2) populates both in one
roundtrip. The COSF `cmdCustom` does this — see
`CorporateShipmentFlowActivityService.cmdCustom`.

### 20.5 Detail-screen `cmd_create` must inject the parent's PK into the dialog data

The Add dialog on a detail screen only carries the editable fields
(trader, type, times) — not the parent's PK columns (shipmentFlow).
But `AbstractCrudActivityService.cmdCreate` calls `idFromData(data, …)`
on the dialog's data map. Without an override, that throws
"Shipment flow is required" because the dialog never sent it.

Solution: override `cmdCreate` (and `cmdUpdate`, `cmdCopy`) and inject
the parent's PK from `request.parentModel` into the dialog's data map
before delegating to `super`. The toolbar button must have
`parentData: true` so the engine ships `parentModel`. See
`CorporateShipmentFlowDetailsActivityService.withParentInjected`.

### 20.6 No `clickForDetail` on picker workflow grids

A picker workflow (`trader.prompt` here) returns the picked row's
values to the caller via the row's `onClick` handler in `DynamicGrid`,
which reads `screen.returnTo` (populated when the caller's prompt
button navigated to the picker). The handler runs **only if no cell-level
`clickForDetail` is active** — `clickForDetail: true` on a column makes
the engine fire `cmd_detail` on the picker's own workflow when that
column is clicked, which the read-only picker activity can't handle.

So picker grids must have **no `clickForDetail` on any column**. Every
cell click bubbles up to the row's onClick, which fires the
return-to-parent navigation. The bug looked like "clicking the trader
column does nothing while clicking other cells works".

### 20.7 The picker's `cmd_search` button needs `local: false`

Picker workflows typically don't show a search-criteria dialog (the
user wants to see all rows immediately). Set the picker's Search button
to `local: false` so clicking it goes straight to the server with an
empty criteria map.

Alternatively, set `local: true` and the engine opens a criteria
dialog — the user clicks Search inside it to fire the actual query.
Both shapes work; the local=false path is one fewer click.

### 20.8 Master-detail `cmd_create` / `cmd_copy` need the saved row in `.model` (singular)

`EntityDialog.tsx` lines 341-356 snapshot `masterDetail` before the
save, then on success reads `modelHolders[""].model.data` (singular)
and *appends* it onto the parent's children grid client-side. The
plural slot is ignored on this path. Without the singular shape the
master-detail append branch silently no-ops and the children grid
stays stale.

Fix: in the detail activity, override `cmdCreate` / `cmdCopy` to wrap
the base class's `.models[0]` response back into a `.model` (singular)
envelope via `AbstractCrudActivityService.asSingletonModel`. See
`CorporateShipmentFlowDetailsActivityService.cmdCreate` for the canonical
wiring.

**Note: this applies to `cmd_create` / `cmd_copy` only.** `cmd_update`
has a different contract — see §20.10. Don't blanket-apply
`asMasterDetailSingleton` to every write verb; it'll break Update.

Don't change `singleEnvelopeResponse` itself — it would silently
downgrade every single-grid screen's slot type from `"grid"` to
`"form"` and break their grid merge.

`cmdDelete` still needs the heavier parentChild refresh because the
engine has no "remove the deleted rows" client-side branch for master
detail. Use `AbstractCrudActivityService.refreshedChildren(request,
requeryChildren)` — the base class wraps the supplied envelopes as a
parentChild response with the master read from `request.parentModel`.

### 20.10 `cmd_update` returns `actionType: "update"` + `componentType: "grid"` + echoed `rowID`

`cmd_update` does **not** share `cmd_create`'s singleton contract. Its
right shape is:

```
modelHolders[""] = {
  componentType: "grid",
  actionType:    "update",
  models:        [savedRowWithRowIdEchoed],
}
```

The engine's `applyResponse` routes `actionType: "update"` through
`mergeRowsByKey` (`ScreenContext.tsx` line ~1737) which finds the
existing row by `rowID` and replaces it in-place. The client stamped
`rowID` on every row at grid-load time (via `assignRowIDsIfMissing`),
the toolbar's `performInlineUpdate` ships it back inside `data`, and
the activity has to echo it on the saved row so the merge matches.

Without echoing rowID, `mergeRowsByKey` finds no match and *appends*
the saved row to the grid — the user sees the row both at its
original position (with stale values) AND appended at the bottom
(with the new values).

Without the `actionType: "update"` tag, the engine falls into the
default `actionType: "none"` branch and **full-replaces** the grid
with the single saved row — every other child disappears. For single-
grid screens this is masked by a follow-up `queryClient.invalidateQueries(['grid-search', workflow])`
refetch that repopulates; for master-detail screens (CSFD) there is
no such refetch and the grid stays collapsed to one row.

Without `componentType: "grid"`, a singular-`.model` response makes
the engine's `keepParentChild` logic merge the saved child row's data
into the **master form** instead — the canonical "Update broke
because asMasterDetailSingleton was applied to the wrong verb" bug.

This contract is baked into `AbstractCrudActivityService.cmdUpdate` +
`buildSavedRowEnvelope` + `gridUpdateResponse` — every activity that
extends the base class gets it by default. Don't override `cmdUpdate`
to re-shape the response unless the screen has a genuinely different
contract.

**Multi-row inline-edit Update**: when the user inline-edits multiple
rows and clicks Update, the toolbar's `performInlineUpdate` batches
every edited row into `modelHolders[""].models[]`. The base class's
`cmdUpdate` iterates over the full list (one DB save per envelope) and
ships back one saved envelope per row, each with its own `rowID`
echoed. The whole batch runs in a single transaction (`@Transactional`
on `cmdUpdate`), so if any row's `beforeUpdate` validation fails the
earlier saves roll back. Reading only the first envelope via
`singleModelData` — the original base-class shape — silently dropped
every edited row after the first; the multi-row iteration is the fix.

### 20.11 `setMasterDetail` must stamp `rowID` on keyless children

`EntityDialog`'s post-`cmd_create` branch appends the freshly-saved row
to the children grid via
`setMasterDetail({ master, children: [...prev, echoed] })`. The
`echoed` row comes straight from the server response — and the server
**cannot** stamp a useful `rowID` because `rowID` is a client-side
grid concept (see `ScreenContext.assignRowIDsIfMissing`). CSnx's GWT
backend explicitly does `dynamicModel.setRowID(null)` before
responding; demo's backend does the same by omission.

If `setMasterDetail` accepts the keyless row as-is, a subsequent
inline-edit Update on that same row ships the row's data back to the
backend without a rowID, the backend's response echoes no rowID, and
`mergeRowsByKey` finds no key match — so the engine *appends* the
saved row a second time. The user sees the row duplicated.

Fix: `setMasterDetail` stamps `rowID` on any keyless child via
`stampMissingRowIds`, continuing numbering from one above the max
rowID already in the children so keys don't collide with rows loaded
via `cmd_search` / `cmd_details`. After this, the row carries the
same `rowID` from the moment it's appended; the next Update merges
in-place via `mergeRowsByKey`.

This is an engine-side fix (the backend genuinely has no useful
`rowID` to inject), so it's the **exception** to the "fix backend
first" rule in `docs/activity-services.md` — when the bug is in the
client-side row-key book-keeping, no backend response shape can paper
over it.

### 20.9 `cmd_copy` must strip the source row's identity PK column

The engine's Copy popup seeds the dialog with the source row's **full**
field set, including hidden / auto-generated columns the dialog
doesn't render. When the activity's `idFromData` reads the
auto-generated id from the seeded data, `validateDuplicate` finds the
source row by its own id and rejects with "Record already exists".

Fix: clear the identity PK column(s) from the dialog data before
`super.cmdCopy` (which delegates to `cmdCreate`) runs. The base class
exposes `withMutatedData(request, mutator)` for this — the activity
writes a tiny `withInsertContext` wrapper that calls
`withMutatedData(request, data -> { ... data.remove("shipmentFlowId"); })`.
For `ShipmentFlowDetail` see `CorporateShipmentFlowDetailsActivityService.withInsertContext`.
The same wrapper also injects the parent's `shipmentFlow` (read via
`parentValue(request, "shipmentFlow")` so it works even when the
toolbar ships the request without `parentModel`).

---

## 21. Implementation status

The engine described in this file is the current contract — back-end
endpoints and front-end runtime are both wired and exercised by:

- **RPTM** (`reportText.maintenance`) — simple single-table CRUD.
- **COSF + CSFD + TRDP** (`corporateShipmentFlows`,
  `corporateShipmentFlowDetails.maintenance`, `trader.prompt`) —
  full master-detail with parent-context navigation, popup detail
  form, and trader picker that returns via `linkItem.fieldMapper`.

Known unfinished areas:

- **Charting / report rendering** — schema slots exist; no renderer.
- **`cmd_upload` file flow** — `UploadDialog` exists; no demo workflow
  exercises it.
- **`FilterMetadata` UI** — wire round-trips; no popup renderer wired.

Audit reference: `audits/2026-05-26/infrastructure/front-end.md`.
