# CLAUDE.md

Reference Spring Boot module stack (CSNX-13935) — the modern architecture
replacing the legacy CSnx/GWT platform. Polyglot project: Java backend +
React frontend in one repo, postgres alongside. The first concrete consumer;
future modules at `C:\software\projects\modules\<name>` clone this template.

Three independently built containers (see `docker-compose.yml`):
`postgres` (`postgres:16-alpine`, port 5432) → `backend` (Spring Boot fat-jar,
port 8080) → `frontend` (nginx serving the Vite build, port 8081 → 80, with
`/api` reverse-proxied to the backend). Each layer scales / redeploys on its
own; nothing is bundled.

## Layout

```
backend/        Maven Spring Boot project, Java 21
frontend/       Vite + React + TypeScript
docker-compose.yml
```

Open in one Claude Code session at the repo root. Cross-cutting changes
(DB → entity → DTO → API → form) are common — Claude needs to see both ends.

## Stack

| Layer | Choice |
|---|---|
| Java | 21 (LTS) |
| Framework | Spring Boot 3.5 (Spring 6, Jakarta) |
| Build | Maven |
| ORM | Hibernate via Spring Data JPA + `JpaSpecificationExecutor` |
| Migrations | Flyway (`backend/src/main/resources/db/migration/V*__*.sql`) |
| Validation | Jakarta Bean Validation (Hibernate Validator) |
| Auth | Spring Security + JWT access (1h) + refresh cookie (7d, httpOnly) |
| DTO mapping | MapStruct (compile-time) |
| API docs | springdoc-openapi → `/v3/api-docs`, `/swagger-ui.html` |
| Logging | SLF4J + Log4j 2 (NOT Logback, NOT Log4j 1.x) |
| DB | PostgreSQL (prod + dev), Testcontainers Postgres for tests |
| Testing | JUnit 5 + Mockito + AssertJ + Spring Boot test slices |
| Frontend | React 18 + TS strict, Vite, shadcn-style primitives (Tailwind 3) |
| Frontend state | TanStack Query (server state), react-hook-form + zod (forms) |
| Frontend client | axios (`frontend/src/lib/api.ts`) with bearer + auto-refresh on 401 |

## Run

```powershell
# Postgres (one-shot)
docker run --rm -d --name demo-pg -e POSTGRES_DB=demo -e POSTGRES_USER=demo -e POSTGRES_PASSWORD=demo -p 5432:5432 postgres:16-alpine

# Backend (needs Java 21)
cd backend ; ./mvnw spring-boot:run

# Frontend (needs Node 20.19+ / 22+ — fnm-managed Node 24 LTS is set up)
cd frontend ; npm run dev   # http://localhost:5173

# Tests
cd backend ; ./mvnw test    # unit + Testcontainers integration

# E2E (Playwright — the standing verification for demo work; needs the
# platform + demo compose stacks up. No env vars: auth.setup signs in as the
# seeded `wcstest`/`wcstest123` e2e admin — `wcs`/`wcs123!` is the HUMAN dev
# login only. User matrix: pom/frontend/tests/e2e/README.md)
cd frontend ; npx playwright test

# Full stack (3 containers: postgres + backend + frontend)
docker compose up --build   # frontend http://localhost:8081, backend :8080, db :5432
```

## Conventions — backend

- **No Lombok.** Hand-write getters/setters on entities. DTOs are `record`s —
  modern Java covers that case.
- **Entities, repositories, optimistic locking**: see
  [docs/dao-patterns.md](docs/dao-patterns.md). The short version:
  `BaseEntity` → `MaintainableDateTimeBaseEntity` →
  `MaintainableTranUserBaseEntity`; every table carries
  `update_serial BIGINT NOT NULL` (Java field `updateSerial`, inherited
  from `BaseEntity`); repositories extend `BaseRepository<T, ID>`. No
  per-entity `equals`/`hashCode` or `setNullToDefaults` — CSnx patterns
  demo doesn't need (DB-side `DEFAULT` clauses + identity equality
  cover the cases). See [docs/decisions.md § 9](docs/decisions.md).
- **Activity services** for engine workflows: see
  [docs/activity-services.md](docs/activity-services.md). Extend
  `AbstractCrudActivityService<T, ID>` for CRUD-shaped screens.
- **Migrations**: see [docs/migrations.md](docs/migrations.md). Flyway under
  `backend/src/main/resources/db/migration/V<n>__<snake_case>.sql`. Don't
  edit a released migration. Screen JSON / menu / lookups are *not* shipped
  via Flyway — the engine registrar registers `screens/*.json` +
  `registry/*.json` with the central platform metadata store on boot; role
  grants live platform-side (`module_grant`).
- **Three layers: ActivityService → ActivityBean → Repository.** Mirrors
  CSnx's `BaseActivityService → BusinessHelper.getXxxActivity() → DAO`.
  The service does wire-shape work (toData/fromData, command routing,
  auth-context extraction). The bean owns business logic (`validate`,
  `beforeSave`, `idOf`, `search`, custom finders). Repository access
  lives **only on the bean**; no `*ActivityService` class touches a
  repository directly. No `DAOHelperBean` facade — Spring DI is enough.
- **Package layout (mirrors CSnx):** activity services + base infra
  in `za.co.csnx.demo.service.activity.*`; activity bean interfaces +
  impls + base bean in `za.co.csnx.demo.business.activity.*`. There
  is no `engine` package on the backend any more (`web.dto.engine`
  remains for wire DTOs).
- **Beans are interface + impl.** Each bean ships as an `XxxActivity`
  interface (extends `CrudActivity<T, ID>`) and an `XxxActivityBean`
  impl (extends `AbstractCrudActivityBean<T, ID>` + implements
  `XxxActivity`). Services and cross-bean wirings constructor-inject
  the **interface** type, never the impl class. Mirrors CSnx's
  `WaddActivity` + `WaddActivityBean` split.
- **No `idFromData` / field-by-field PK extraction.** Build the whole
  entity from the wire via `fromData(data, entity)` (reflection-backed
  default copies every property including PK fields, skipping the
  audit/version/company-scope columns), then derive the PK via
  `bean.idOf(entity)`. Mirrors CSnx's `copyDynamicModel(model,
  EntityType.class)` pattern. The same applies to search: build a
  probe entity from the criteria map and call `bean.search(probe)` —
  no per-field criterion extraction.
- **Validation = real business rules only.** DB-level `NOT NULL`
  handles "X is required" rejections; don't duplicate them in
  `validate`. The bean's `validate(entity, mode)` only enforces ranges,
  cross-field rules, and lookup-existence checks (e.g. "trader must
  exist in the Trader table"). Mirrors CSnx's
  `WaddActivityBean.isValid` surface.
- **`@Transactional` is method-level only — never class-level.** Each
  public bean method declares its own `@Transactional` or
  `@Transactional(readOnly = true)`. Reason: the read/write boundary is
  explicit at every call site, survives method extraction, and matches
  CSnx's pattern.
- **Controllers**: validate with `@Valid`, map entities → DTOs via MapStruct,
  return `ResponseEntity<DTO>` or `DTO`. Errors flow through
  `GlobalExceptionHandler` → RFC 7807 `ProblemDetail`.
- **Auth**: `JwtAuthFilter` reads `Authorization: Bearer …`, sets
  `SecurityContext`. Refresh token lives in `refreshToken` httpOnly cookie
  scoped to `/api/auth`. `AuditorAware` reads username from the security
  context for audit fields.
- **Tests**: prefer slice tests (`@WebMvcTest`, `@DataJpaTest`) for fast loops;
  `@SpringBootTest` only for cross-cutting integration. Use Testcontainers
  Postgres, not H2 (HQL/dialect divergence isn't worth the risk).
- **Logging**: `LoggerFactory.getLogger(MyClass.class)`. Don't reach for the
  Lombok `@Slf4j` — it's gone.

## Conventions — frontend

- **shadcn primitives** in `frontend/src/components/ui/`. Compose them; don't
  reinvent. They're written so a future metadata-driven engine can use them as
  leaf renderers (see "Future direction" below).
- **Server state via TanStack Query.** Never store API responses in component
  state.
- **Forms**: `react-hook-form` + `zod` resolver. Inline error messages under
  fields.
- **API**: import from `@/lib/api`. Don't call `fetch`/`axios` directly in
  components.
- **Auth**: `useAuth()` from `@/lib/auth`. Access token lives in memory; refresh
  is automatic on 401 via the axios interceptor.
- **Routing**: `frontend/src/router.tsx` defines public/protected route
  wrappers. New protected pages go under the `<ProtectedRoute />` group.
- **Types**: TS strict mode. No `any`. Generated OpenAPI client is the
  long-term plan; for now hand-typed DTOs in `lib/api.ts` mirror backend.
- **Responsive — every component and page must work on desktop, tablet, and
  mobile.** Mobile-first: write base utilities for the smallest viewport,
  add `sm:` / `md:` / `lg:` variants to scale up. Test layouts at ~360px
  (phone), ~768px (tablet), and ~1280px (desktop) before considering a page
  done. Don't ship a layout that requires horizontal scrolling on phones,
  hides controls behind tiny click targets, or stacks badly because flex
  rows weren't given a `flex-col sm:flex-row` fallback.
  - **Tables**: plain `<table>` overflows on phones. Default pattern is
    "shadcn `Table` at `md+`, card-list at `<md`". Horizontal scroll is the
    fallback; column-hiding at narrow widths is the upgrade. TanStack Table
    is the headless library to pair with shadcn's `table.tsx` once needed.
  - **Mobile primitives**: `Sheet` (drawer) replaces `Dialog` for nav and
    long forms on phones; `DropdownMenu` for compact actions. Add them via
    shadcn when first needed.
- **Add shadcn primitives via CLI — never hand-roll.** `frontend/components.json`
  is already wired (Vite + TS, `@/` alias, `slate` base, `lucide` icons,
  `cssVariables: true`). All primitives in `components/ui/` are upstream-tracked
  via `npx shadcn@latest add …`, so future `--diff` updates stay clean. When a
  page needs a new primitive, install it; don't write one from scratch.
  - **Run from `frontend/`**: `npx shadcn@latest add <name>` (one or more).
    Replacing an existing hand-rolled file: add `--overwrite --yes`.
  - **Don't run `npx shadcn@latest init`.** It would clobber the customised
    `tailwind.config.js` and `index.css` (semantic `warning` / `info` tokens).
    `components.json` is the only thing init produces that we need, and it's
    already committed.
  - **Patch CLI output for two recurring violations.** Upstream shadcn ships
    `space-y-*` / `space-x-*` in some primitives (e.g. `card.tsx`,
    `dialog.tsx`); convert to `gap-*` after every install. Don't ship a
    primitive with `space-*` left in.
  - **Canonical mapping** (install when first needed):
    - **`Field` / `FieldGroup`** for forms — `npx shadcn@latest add field`.
      Use instead of raw `<div>` + `<Label>` + `<Input>`. Validation surfaces
      via `data-invalid` on `Field` + `aria-invalid` on the control.
    - **`Alert`** for inline callouts — `npx shadcn@latest add alert`. Don't
      build styled `<div>`s for "Account created" / "Email already verified" /
      similar.
    - **`Table`** + TanStack Table for tabular data —
      `npx shadcn@latest add table`. Pair with the `<md` card-list pattern
      from the Responsive section.
    - **`Sheet`**, **`DropdownMenu`**, **`Skeleton`**, **`Badge`**, **`Empty`**,
      **`Separator`** — same rule, install when first needed.
- **Don't import a primitive that hasn't been installed.** Check
  `components/ui/` first; if missing, run `npx shadcn@latest add <name>`.
- **No raw colour utilities for status (`text-amber-500`, `bg-blue-50` etc.).**
  Use the semantic tokens in `index.css`: `text-destructive`, `text-warning`,
  `text-info`, plus their `-foreground` pairs and `bg-*` variants. If a new
  status colour is needed, add the HSL pair to `index.css` (light + dark) and
  expose it in `tailwind.config.js` rather than dropping a Tailwind palette
  colour into a component.
- **Spacing inside flex/grid containers**: `flex flex-col gap-*`, never
  `space-y-*`. (Already enforced — keep it that way when adding new layouts.)

## API

| Method | Path | Auth |
|---|---|---|
| POST | `/api/auth/register` | none |
| POST | `/api/auth/login` | none |
| POST | `/api/auth/refresh` | refresh cookie |
| POST | `/api/auth/logout` | none |
| GET | `/api/me` | JWT |

## MCP servers

- **GitLab** — user-scope, available automatically. Use for issues / MRs / repo
  ops on `gitlab.com/WorldwideChainStores/...`.
- **Atlassian (Jira / Confluence)** — declared in `.mcp.json` at the repo root.
  Use `mcp__atlassian__*` tools for tickets, search, etc. Cloud ID:
  `32b38c14-f87d-48a0-a8f4-43d094f9b637` (worldwidechainstores).
- **Postgres (`crystaldba/postgres-mcp`)** — declared in `.mcp.json`, runs via
  Docker against the local `demo-postgres-1` container (connects through
  `host.docker.internal:5432`, db/user/pass = `demo`). Configured in
  **read-write** mode (`--access-mode=unrestricted`) — flip to `restricted` if
  read-only is wanted. Use `mcp__postgres__*` tools for live SQL, schema
  inspection, and query analysis. Requires the compose stack (or the one-shot
  `demo-pg` container) to be running.
- **Azure (`@azure/mcp`)** — official Microsoft Azure MCP server, declared in
  `.mcp.json`. Runs via `npx`; uses cached Azure CLI tokens for auth, so
  `az login` must have been run on the host first. Use `mcp__azure__*` tools
  for live resource queries (Resource Graph), Container Apps revisions, log
  streams, Bicep / ARM deployments, and Key Vault / managed identity
  inspection. Paired with the `azure-prepare` / `azure-validate` /
  `azure-deploy` / `azure-diagnostics` skills for guided deployment
  workflows.

## Metadata-driven UI engine

**The app shell, module switcher, portal and smart-nav are shared engine code —
don't hand-roll or copy them.** The header + sidebar come from the engine's
`EngineAppShell` (`@alessiohchain/csnx-engine/shell/EngineAppShell`); this
module's `frontend/src/shell/AppShell.tsx` is a ~10-line wrapper injecting the
theme-picker slot. The in-header **module switcher** + the platform **portal**
read the access-token `modules` claim and navigate cross-origin via silent SSO.
The **smart-nav assistant** ("Ask or search…") is the optional `csnx-engine-ai`
Maven artifact, rendered by the shell gated by `features.smartNavigation`; Demo
has it wired (dep + `csnx.engine.ai.llm.*`), and its SPI defaults are
boilerplate-free (Demo keeps only a custom `QueryLogStore`). Engine changes go
in the platform repo → bump → re-vendor. See `platform/docs/engine-sharing.md`
(Shared application shell + Shared smart-nav).

The metadata-driven engine is wired and exercised by two seeded screen
families: RPTM (simple single-table CRUD) and COSF + CSFD (master-detail
with `cmd_details` server-roundtrip navigation, a popup detail-form, and
a trader picker workflow). See **[docs/engine.md](docs/engine.md)** for the
full contract — wire shape of `/api/metadata` / `/api/process` /
`/api/lookup/init`, the `MetadataHolder` schema, `modelType` routing,
validation cascade, activity-service template methods, dialog vocabulary,
lookup bootstrap, fastpath/menu/role model, and the recipe for adding a
new screen.

The **canonical master-detail example is COSF/CSFD**: COSF's `cmd_details`
verb is a custom `cmdCustom` override that loads the children server-side
and returns a `componentType: "parentChild"` envelope (master in
`modelHolders[""].model`, children in `modelHolders[""].models[]`) so the
engine populates both the header form and the detail grid in one
roundtrip. CSFD's `cmd_search` / `cmd_create` / `cmd_update` use
`parentData: true` to ship the master back as `parentModel`; the activity
injects `shipmentFlow` from there into the dialog data so composite-PK
reconstruction succeeds.

The **canonical singleton-edit example is WSPM** — one row per company,
no Add/Delete/Copy. The activity extends `AbstractEngineActivity`
directly (sibling of `AbstractCrudActivityService`, mirroring CSnx's
`WaddActivityService_WSPM extends BaseActivityService`) and implements
only `cmdSearch` (load-or-default) + `cmdUpdate` (upsert). The screen
JSON's `action: "cmd_search"` fires the load on mount.

**Section grouping inside one form block is the pattern.** Tag each
field with `"sectionName": "..."` in the screen JSON; the engine's
`DynamicForm.groupBySection` buckets fields by section and renders each
bucket inside a shadcn `<Collapsible>` when more than one named section
exists. Mirrors CSnx's SWET `sectionItem` attribute. Multiple
`kind: form` metadata blocks under one workflow are a separate
mechanism (for genuinely unrelated form groups, e.g. CSFD's header +
detail-popup); they each carry their own React-Hook-Form context and
don't share state, so don't use them for sections of one logical form.

**Screen authoring convention:** one JSON file per workflow under
`backend/src/main/resources/screens/<workflow>.json`. On boot the engine
registrar registers each file with the **central platform metadata store**
(`module_cd='DEMO'`, hashing the payload to short-circuit unchanged files);
the module reads metadata back via `PlatformMetadataSource`. Iterating on a
screen = edit the JSON, restart — no Flyway migration. A brand-new fastpath
needs a `registry/menu.json` entry plus a **platform-side** `module_grant`
row (deny-by-default; the ADMIN role's `'*'` wildcard covers admins).
`_schema.json` in
the same directory gives VS Code / IntelliJ autocomplete via the
`$schema` reference at the top of each file.

shadcn primitives are intentionally simple so the engine's `FieldRenderer`
can dispatch to them as leaf renderers. Hand-coded screens are still
supported (e.g. login, change-password) but anything CRUD-shaped should go
through the engine.

## Reference docs

Deep dives live under `docs/`. Read the one relevant to the task you're
on — CLAUDE.md keeps only the rules; the docs carry the explanations and
worked examples.

| File | Purpose |
|---|---|
| [docs/engine.md](docs/engine.md) | System-of-record for the metadata engine — `/api/metadata`, `/api/process`, `/api/lookup/init` wire shapes, `MetadataHolder` schema, validation cascade, master-detail landmines. |
| [docs/activity-services.md](docs/activity-services.md) | How to write an activity service — `AbstractCrudActivityService` hooks, response-envelope shapes, the four worked examples (RPTM/COSF/CSFD/TRDP). |
| [docs/dao-patterns.md](docs/dao-patterns.md) | Entity + repository conventions — `BaseEntity` hierarchy, `@IdClass` composite PKs, `@Version updateSerial`, `BaseRepository`, sequence generators. |
| [docs/migrations.md](docs/migrations.md) | Flyway conventions — file naming, `demo`, CSnx-aligned column shapes, what Flyway ships vs what the screen-metadata seeder owns. |
| [docs/architecture.md](docs/architecture.md) | Single-module wiring end-to-end — backend package layout, frontend source tree, container architecture, auth flow. |
| [docs/decisions.md](docs/decisions.md) | Rationale behind every stack choice. Read before proposing a stack change ("add Lombok back?", "switch to Logback?", "use H2 for tests?"). |
| [docs/platform-architecture.md](docs/platform-architecture.md) | Demo as the single-module template; sketch of the multi-module future. |
| [docs/demo-ops.md](docs/demo-ops.md) | GCP deployment runbook. |

## Plan / ticket

Ticket: <https://worldwidechainstores.atlassian.net/browse/CSNX-13935>. The
origin plan and follow-on audit work live under that ticket and its linked
epic CSNX-14044 (see `audits/2026-05-26/jira-ticket-mapping.md` for the
audit-driven sub-tasks).
