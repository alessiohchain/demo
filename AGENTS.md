# AGENTS.md — Demo

Reference Spring Boot module stack (CSNX-13935) — the template future modules
at `C:\software\projects\modules\<name>` clone. Java 21 backend + React
frontend in one repo, postgres alongside; renders through the shared CSnx UI
engine. **The engine is a SHARED artifact — do not edit it here**; engine
changes go in the platform repo (`../platform`, `packages/engine` +
`engine-spring`) → bump → publish → re-vendor here. See
`../platform/docs/engine-sharing.md`.

Fleet-canonical docs live in the sibling checkout `../platform/docs/` — clone
`platform` alongside this repo.

## Conventions

Backend, frontend, screen-authoring and master-detail conventions are
fleet-canonical: **read
[../platform/docs/module-conventions.md](../platform/docs/module-conventions.md)**
before writing code here. Module package root: `za.co.csnx.demo`
(`service.activity.*` / `business.activity.*`; no backend `engine` package —
`web.dto.engine` remains for wire DTOs).

## Ports & run

Fleet ports registry: [../platform/docs/fleet.md](../platform/docs/fleet.md).
This repo: backend **8092**, frontend **8085**, compose db **5435**; the dev
datasource default is `localhost:5432/demo`, so the one-shot dev container
runs on 5432.

```powershell
# Postgres (one-shot, dev)
docker run --rm -d --name demo-pg -e POSTGRES_DB=demo -e POSTGRES_USER=demo -e POSTGRES_PASSWORD=demo -p 5432:5432 postgres:16-alpine

cd backend ; ./mvnw spring-boot:run     # Java 21; serves on :8092
cd frontend ; npm run dev               # http://localhost:5173
cd backend ; ./mvnw test                # unit + Testcontainers

# E2E (Playwright — the standing verification for demo work; needs the
# platform + demo compose stacks up. No env vars: auth.setup signs in as the
# seeded `wcstest`/`wcstest123` e2e admin — `wcs`/`wcs123!` is the HUMAN dev
# login. Full user matrix: ../platform/docs/testing.md)
cd frontend ; npx playwright test

docker compose up --build               # frontend :8085, backend :8092, db :5435
```

## Auth — relying party on the platform IdP

Demo has **no auth endpoints of its own**. Login happens on the platform IdP
(OIDC code+PKCE); the SPA bootstraps via silent SSO and reads the
access-token `modules` claim (`frontend/src/app/auth/platformSso.ts`,
`moduleClaim.ts`, `AuthProvider.tsx`). The backend accepts platform-issued
RS256 tokens only; its controllers are Lookup / Metadata / Process /
SessionBootstrap / TestCall. `AuditorAware` reads the username from the
security context for audit fields.

## Stack deltas

Base stack: [../platform/docs/fleet.md](../platform/docs/fleet.md) §Base
stack. Demo-specific rows: Auth = relying party on the platform IdP (above);
Frontend client = `frontend/src/app/api/client.ts` (import it — never call
`fetch`/`axios` directly in components); React 18 / Tailwind 3.

## Migrations

Timestamp versions, undo scripts, DB2 portability — rules in
[../platform/docs/migrations.md](../platform/docs/migrations.md) (a local
template copy sits at [docs/migrations.md](docs/migrations.md)). This repo:
schema `demo`, frozen sequential band `V1`–`V22` (undo coverage enforced from
`V20260808000000` and on anything above `V23`), engine band `V9000+`. Screen
JSON / menu / lookups are *not* shipped via Flyway — see Screens below.

## Screens

Metadata publication is the **registrar** variant (`module_cd='DEMO'` →
central platform metadata store, read back via `PlatformMetadataSource`;
fastpaths need a `registry/menu.json` entry + a **platform-side**
`module_grant` row; lookup families in `registry/lookups*.json`). Both
variants: `../platform/docs/module-conventions.md` §Screen authoring.

Demo's seeded screens are the template's **worked examples** (removed from
the *platform* repo on 2026-06-13, deliberately retained here):

- **RPTM** (`reportText.maintenance`) — simple single-table CRUD.
- **COSF/CSFD** (`corporateShipmentFlows` + details) — master-detail:
  `cmd_details` returns a `parentChild` envelope; the child popup ships the
  master back via `parentData: true`. (The fleet's canonical *live* instance
  is POM's `purchaseOrder` — contract in
  `../platform/docs/module-conventions.md` §master-detail.)
- **WSPM** (`sysParameters.maintenance`) — singleton load-or-default edit:
  extends `AbstractEngineActivity` with only `cmdSearch` + `cmdUpdate`, the
  screen firing `action: "cmd_search"` on mount.
- **TRDP** (`trader.prompt`) — picker workflow.
- Plus the engine-provided integration screens SCLG (`integration.callLog`)
  and SVCF (`integration.serviceConfig`).

The app shell, module switcher, portal and **smart-nav** are shared engine
code — don't hand-roll (`EngineAppShell`; demo has smart-nav wired: dep +
`csnx.engine.ai.llm.*`, custom `QueryLogStore` only). Full engine contract +
the recipe for a new screen: [docs/engine.md](docs/engine.md).

## MCP servers

Common set (GitLab, Atlassian, Azure): `../platform/docs/fleet.md` §MCP
servers. Postgres MCP here targets the compose container `demo-postgres-1`
via `host.docker.internal:5435`, db/user/pass `demo` — the compose stack must
be up (the one-shot `demo-pg` on 5432 is NOT what it connects to).

## Engine version

Vendored as a `file:` tarball in `frontend/vendor/` — publishing moves
nothing here, bumping publishes nothing. Never assume a version; check with
the drift commands. Procedure:
[../platform/docs/engine-versioning.md](../platform/docs/engine-versioning.md).

## Reference docs

Fleet-canonical: `../platform/docs/{module-conventions, fleet,
engine-versioning, migrations, engine-sharing, testing}.md`. Module docs
(template copies note their provenance):

| File | Purpose |
|---|---|
| [docs/engine.md](docs/engine.md) | System-of-record for the metadata engine — wire shapes, `MetadataHolder` schema, validation cascade, master-detail landmines. |
| [docs/activity-services.md](docs/activity-services.md) | How to write an activity service — hooks, envelope shapes, the worked examples (RPTM/COSF/CSFD/TRDP — live in this repo). |
| [docs/dao-patterns.md](docs/dao-patterns.md) | Entity + repository conventions. |
| [docs/migrations.md](docs/migrations.md) | Flyway conventions — schema `demo`; the canonical rules live in `../platform/docs/migrations.md`. |
| [docs/architecture.md](docs/architecture.md) | Single-module wiring end-to-end. |
| [docs/decisions.md](docs/decisions.md) | Rationale behind every stack choice. Read before proposing a stack change. |
| [docs/platform-architecture.md](docs/platform-architecture.md) | Demo as the single-module template; the multi-module future. |
| [docs/demo-ops.md](docs/demo-ops.md) | GCP deployment runbook. |

## Tickets & attachments

Origin ticket: CSNX-13935 (linked epic CSNX-14044; audit sub-tasks:
`audits/2026-05-26/jira-ticket-mapping.md`). Attachment transfers use the
`team-workflow` plugin script (`atlassian-attachments.js`) — usage in
`../platform/docs/fleet.md` §Tickets & attachments.

## Maintaining these instructions

`CLAUDE.md` and `GEMINI.md` are one-line `@AGENTS.md` imports — **never write
content into them**. New rules, context or lessons go into the most specific
reference doc for the topic (under `docs/`), with a pointer added here if
agents need to know it exists. Add text directly to this file only when it
must ALWAYS be in an agent's context (a hard rule, a trap index line, a run
command) — this file is deliberately slim, and every line added here is a
line loaded into every session.
