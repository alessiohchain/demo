# AGENTS.md — Demo

Reference Spring Boot module stack (CSNX-13935) — the template future modules
at `C:\software\projects\modules\<name>` clone. Java 21 backend + React
frontend in one repo, postgres alongside; renders through the shared CSnx UI
engine. **The engine is a SHARED artifact — do not edit it here**; engine
changes go in the platform repo (`../platform`, `packages/engine` +
`engine-spring`) → bump → publish → re-vendor here. See
`../platform/docs/engine-sharing.md`. Repo shape, packages, ports, env vars and
demo's CDC/event flows: [docs/architecture.md](docs/architecture.md).

Fleet-canonical docs live in the sibling checkout `../platform/docs/` — clone
`platform` alongside this repo.

## Conventions

Backend, frontend, screen-authoring and master-detail conventions are
fleet-canonical: **read
[../platform/docs/module-conventions.md](../platform/docs/module-conventions.md)**
before writing code here. Module package root: `za.co.csnx.demo`
(`service.activity.*` / `business.activity.*`; no backend `engine` package).

**Data access** follows the fleet rules — no load-then-reduce in Java, JDBC
only for a listed reason:
[../platform/docs/dao-patterns.md](../platform/docs/dao-patterns.md#never-load-rows-and-then-reduce-them-in-java)
and [§JDBC is the exception](../platform/docs/dao-patterns.md#jdbc-is-the-exception);
enforced here by `DataAccessTest`.

## Ports & run

Fleet ports registry + run template + the `JAVA_HOME` gotcha:
[../platform/docs/fleet.md](../platform/docs/fleet.md). This repo: backend
**8092**, frontend **8085**, compose db **5435**, Vite **5175**; the dev
datasource default is `localhost:5432/demo`, so the one-shot dev container
runs on 5432.

```powershell
# Postgres (one-shot, dev)
docker run --rm -d --name demo-pg -e POSTGRES_DB=demo -e POSTGRES_USER=demo -e POSTGRES_PASSWORD=demo -p 5432:5432 postgres:16-alpine

cd backend ; ./mvnw spring-boot:run     # Java 21; serves on :8092
cd frontend ; npm run dev               # http://localhost:5175
cd backend ; ./mvnw test                # unit + Testcontainers

# E2E (Playwright — the standing verification for demo work; needs the
# platform + demo compose stacks up. No env vars: auth.setup signs in as the
# seeded `wcstest`/`wcstest123` e2e admin — `wcs`/`wcs123!` is the HUMAN dev
# login. Full user matrix: ../platform/docs/testing.md)
cd frontend ; npx playwright test

docker compose up --build               # frontend :8085, backend :8092, db :5435
```

## Auth — relying party on the platform IdP

No auth endpoints of its own; the fleet flow is
[../platform/docs/relying-party-auth.md](../platform/docs/relying-party-auth.md).
Demo's values: audience / SPA client `demo-module`, module code `DEMO`, service
client `demo-service`; controllers are Lookup / Metadata / Process /
SessionBootstrap plus the temporary TestCall (`/api/test/**`, delete after
sign-off).

## Stack deltas

Base stack: [../platform/docs/fleet.md](../platform/docs/fleet.md#base-stack).
Demo-specific rows: Auth = relying party on the platform IdP (above);
Frontend client = `frontend/src/app/api/client.ts` (import it — never call
`fetch`/`axios` directly in components); React 19 / Tailwind 4; postgres image
= postgres 16 + wal2json (`infra/postgres-wal2json`) for WAL CDC.

## Migrations

Fleet rules: [../platform/docs/migrations.md](../platform/docs/migrations.md).
This repo's values — schema `demo` (scripts before `V22` still say
`demoschema` and stay that way), frozen sequential band **`V1`–`V23`**, undo
coverage from `V20260808000000` and on anything above `V23`, engine band
`V9000+`, postgres only: [docs/migrations.md](docs/migrations.md). Screen JSON
/ menu / lookups are *not* shipped via Flyway — see Screens below.

## Screens

Metadata publication is the **registrar** variant (`module_cd='DEMO'` →
central platform metadata store, read back via `PlatformMetadataSource`;
fastpaths need a `registry/menu.json` entry + a **platform-side**
`module_grant` row; lookup families in `registry/lookups*.json`). How the
variants work and the authoring rules:
[../platform/docs/module-conventions.md](../platform/docs/module-conventions.md#screen-authoring);
the engine contract and the recipe for a new screen:
[../platform/docs/engine.md](../platform/docs/engine.md).

Demo's seeded screens are the fleet's **worked examples** — platform's
`engine.md`, `activity-services.md`, `dao-patterns.md` and `migrations.md`
point at this repo's code for them (removed from the platform repo on
2026-06-13, deliberately retained here):

| Screen | Activity service → bean | Shows |
|---|---|---|
| **RPTM** `reportText.maintenance` | `ReportTextActivityService` → `ReportTextActivity` | single-table CRUD, composite PK via `@IdClass` |
| **COSF / CSFD** `corporateShipmentFlows` + `…Details.maintenance` | `CorporateShipmentFlow(Details)ActivityService` → `ShipmentFlowHeaderActivity` / `ShipmentFlowDetailActivity` | master-detail: `cmd_details` returns a `parentChild` envelope; the child popup ships the master back via `parentData: true`; `@SequenceGenerator` on a composite PK |
| **TRDP** `trader.prompt` | `TraderPromptActivityService` → `TraderActivity` | picker workflow returning via `linkItem.fieldMapper` |
| **WSPM** `sysParameters.maintenance` | `SysParametersActivityService` → `SysParametersActivity` | singleton load-or-default edit on `AbstractEngineActivity` (`cmdSearch` + `cmdUpdate` only; the screen fires `action: "cmd_search"` on mount) |

The CSnx-modelled entities (`ReportText`, `ShipmentFlowHeader` /
`ShipmentFlowDetail`, `Trader`, `SysParameters`) extend
`MaintainableTranUserBaseEntity`; the module-native ones (`AppUser`, `Company`,
`AssistantQueryLog`) extend `BaseEntity` directly. Beans are interface + impl
and callers inject the interface (CSFD's bean injects `TraderActivity`). Plus
the engine-provided integration screens SCLG (`integration.callLog`), SVCF
(`integration.serviceConfig`) and CDCC (`integration.cdcConfig`). The fleet's
canonical *live* master-detail is POM's `purchaseOrder`
([§Section grouping & master-detail](../platform/docs/module-conventions.md#section-grouping--master-detail)).

The app shell, module switcher, portal and **smart-nav** are shared engine
code — don't hand-roll (`EngineAppShell`; demo has smart-nav wired: dep +
`csnx.engine.ai.llm.*`, custom `QueryLogStore` only).

## MCP servers

Common set (GitHub, Atlassian, Azure): [../platform/docs/fleet.md](../platform/docs/fleet.md#mcp-servers).
Postgres MCP here targets the compose container `demo-postgres-1` via
`host.docker.internal:5435`, db/user/pass `demo` — the compose stack must be up
(the one-shot `demo-pg` on 5432 is NOT what it connects to).

## Engine version

- **Frontend** (`@worldwidechainstores/csnx-engine`) resolves from **GitHub Packages**
  — a `^` range in `frontend/package.json` plus `.npmrc` (`GH_PACKAGES_TOKEN`;
  the Dockerfile takes it as a BuildKit secret). The old `frontend/vendor/*.tgz`
  tarball is gone; don't reintroduce it.
- **Backend** (`csnx-engine-spring`, `csnx-engine-ai`) resolves from the
  checked-in `backend/vendor-repo` (`scripts/vendor-engine.ps1` after bumping
  `backend/pom.xml`). `csnx-engine-ai` pins its own `csnx-engine-spring`
  transitively and the script vendors that too — why an older version dir stays
  behind alongside the current one.

Never state a version in docs; re-vendoring and drift checks:
[../platform/docs/engine-versioning.md](../platform/docs/engine-versioning.md#consumer--re-vendoring).

## Reference docs

Fleet-canonical docs live only in `../platform/docs/` (index:
[../platform/AGENTS.md](../platform/AGENTS.md#reference-docs)). This repo leans on
[module-conventions](../platform/docs/module-conventions.md),
[engine](../platform/docs/engine.md),
[activity-services](../platform/docs/activity-services.md),
[dao-patterns](../platform/docs/dao-patterns.md),
[migrations](../platform/docs/migrations.md),
[relying-party-auth](../platform/docs/relying-party-auth.md),
[platform-architecture](../platform/docs/platform-architecture.md),
[cdc-event-integration](../platform/docs/cdc-event-integration.md),
[traps](../platform/docs/traps.md) and [testing](../platform/docs/testing.md).

Demo's own docs:

| File | Purpose |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Repo shape, packages, auth values, ports, env vars, CDC + event flows. |
| [docs/migrations.md](docs/migrations.md) | This module's migration values and invariants (schema `demo`, frozen `V1`–`V23`). |
| [docs/decisions.md](docs/decisions.md) | Module ADRs — rationale behind every stack choice. Read before proposing a stack change. |
| [docs/demo-ops.md](docs/demo-ops.md) | GCP deployment runbook. |

## Tickets & attachments

Origin ticket: CSNX-13935 (linked epic CSNX-14044; audit sub-tasks:
`audits/2026-05-26/jira-ticket-mapping.md`). Attachment transfers:
[../platform/docs/fleet.md](../platform/docs/fleet.md#tickets--attachments).

## Maintaining these instructions

Same rules as the platform repo —
[../platform/AGENTS.md §Maintaining these instructions](../platform/AGENTS.md#maintaining-these-instructions):
`CLAUDE.md` / `GEMINI.md` stay one-line `@AGENTS.md` imports, and new content
goes into the most specific doc under `docs/` with a pointer here only if it
must always be in context.
