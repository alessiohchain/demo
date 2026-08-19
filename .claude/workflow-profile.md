# Workflow profile — Demo module

A CSnx-native demo module on the CSnx module platform (Spring Boot backend + React metadata-driven UI engine, Postgres), used as a **reference template** for scaffolding new modules.

This file is the per-project half of the `team-workflow` plugin skills. The skills hold the
process; this file holds this project's facts.

> **Onboarding status: partial.** The build, source-control and local-run sections below are
> filled in from the repo. The Jira-workflow, environment, UAT and Confluence sections are
> marked `TODO (not onboarded)` — this project does not yet run the full ticket lifecycle.
> Run `/setup-workflow-profile` to complete them when it does.

## Identity

- **Issue tracker**: `TODO (not onboarded)` — confirm whether this project's work is tracked in
  Jira (project key `CSNX`, site `worldwidechainstores.atlassian.net`) or elsewhere.
- **Epics**: `TODO (not onboarded)`.
- **Workflow statuses**: `TODO (not onboarded)`.
- **Transitions that require a worklog**: `TODO (not onboarded)`.

## Source control

- **Host**: GitHub — `github.com/alessiohchain/demo.git`. Use the `mcp__github__*` MCP tools for pull requests.
- **Trunk branch**: `main`.
- **Feature branch**: cut from trunk, named after the ticket or a short slug:

  ```bash
  git fetch origin main && git checkout -b <BRANCH> origin/main && git push -u origin <BRANCH>
  ```

- **Epic branches**: `TODO (not onboarded)`.
- **Merge style**: `git merge --no-ff`.
- **Conflict-prone shared files**: `TODO (record them as they are found)`.

## Build & test

| Purpose | Command |
|---|---|
| Compile / fast build (backend) | `cd backend && ./mvnw compile` |
| Package deployable artifact | `cd backend && ./mvnw clean package` |
| Full build gate | `cd backend && ./mvnw clean package` (plus `cd frontend && npm run build` if the UI changed) |
| Run the whole test suite (backend) | `cd backend && ./mvnw test` (unit + Testcontainers integration) |
| Run a single test class | `cd backend && ./mvnw test -Dtest=<ClassName>` |
| Frontend unit tests | `cd frontend && npm test` |
| End-to-end / browser suite | `cd frontend && npx playwright test` |

- **Code-generation rules**: no code generation; **no Lombok** — hand-write getters/setters on entities, DTOs are `record`s.
- **Test conventions**: JUnit 5 + Testcontainers for backend integration tests.
- **Regression suite**: `frontend/tests/e2e/` (Playwright) — a ticket adds one spec.

## Local run

```bash
cd backend  && ./mvnw spring-boot:run
cd frontend && npm run dev              # http://localhost:5173

# Full stack, closer to prod
docker compose up --build               # frontend :8081, backend :8080, db :5432
```

- **URL**: `http://localhost:5173` (dev) or `http://localhost:8081` (compose).
- **Java 21** is required for the backend; **Node 20.19+ / 22+** for the frontend.
- **Database**: Postgres on `:5432` (see `docker-compose.yml` for db/user/password).
- **Login**: sign-in goes through the central platform IdP (OIDC) — the platform stack must be running alongside this module.
- ⚠️ **Docker serves a stale jar** unless `./mvnw package` runs before `docker compose up --build`.

## Environments

| Name | Config | App URL | Database | Deployed by | Constraints |
|---|---|---|---|---|---|
| local | see **Local run** | `http://localhost:5173` | local | developer | — |

- **Default environment for UAT test execution**: `TODO (not onboarded)`.
- **Who deploys the trunk**: `TODO (not onboarded)`.
- **Shared-environment hard constraints**: `TODO (not onboarded)`.
- **How to prove a change is deployed**: `TODO (not onboarded)`.

## Data access

- **Database**: PostgreSQL (see `docker-compose.yml`).
- **Read access**: `TODO (register an MCP server or document the client to use)`.
- **Write/DML access**: `TODO (not onboarded)`.
- **Per-machine config**: `.claude/uat/uat-config.local.json` — create and gitignore it when UAT
  execution starts.
- **Table/naming conventions and query traps**: `TODO (record them as they are found)`.

## Docs & deliverables

**Everything for a ticket lives in one folder, and an epic's subtickets nest inside the
epic's folder.** Below, **`<TICKET_DIR>`** means:

- `docs/tickets/<EPIC>/<TICKET>/` — ticket with an epic parent
- `docs/tickets/<TICKET>/` — standalone ticket

Create any missing folders. Working `.md` files drop the ticket key (the folder carries it);
rendered `.docx` keep it, because those get attached to Jira and downloaded where the folder
name is lost.

| Artifact | Path |
|---|---|
| Implementation plan | `<TICKET_DIR>/plan.md` |
| Epic tracker | `docs/tickets/<EPIC>/epic-plan.md` |
| Developer handover guide | `<TICKET_DIR>/developer-guide.md` → `<TICKET>-developer-guide.docx` |
| UAT handover guide | `<TICKET_DIR>/uat-guide.md` → `<TICKET>-uat-guide.docx` |
| Test plan | `<TICKET_DIR>/testplan.md` |
| Test doc source + evidence | `<TICKET_DIR>/testdoc.md`, `evidence/` |
| Customer test document | `<TICKET_DIR>/Test_Doc_<TICKET>Processing.docx` → `...Final.docx` / `...Failed.docx` |
| Estimate (from a ticket) | `<TICKET_DIR>/estimate.md` → `<TICKET>-estimate.docx` |
| Estimate (from a document) | `docs/estimates/<NAME>-estimate.md` / `.docx` |

- **Legacy locations — read, never move.** Anything produced before this convention stays put
  (`docs/plans/`, `docs/testplans/`, loose `docs/<TICKET>-*` files). If a ticket folder is
  empty, check there before concluding a document does not exist.
- **Retention**: `docs/plans/` is committed and kept; `docs/gaps/` is scratch and not committed.
- **Renderer**: pandoc, `--toc --toc-depth=2` for long guides.
- **Exemplar documents**: none in this repo yet — model on the CSnx repo's exemplars
  (`C:\software\projects\csnx\docs\`), listed in that repo's workflow profile.
- **Pattern guides**: read `CLAUDE.md` and the `docs/` tree in this repo before coding.

## Confluence

- `TODO (not onboarded)` — record the spec space and any customer → space mapping when this
  project starts taking specced work.

## Domain notes & learnings

- This module is a **reference template**: changes here get copied into new modules. See `docs/demo-ops.md`.
- **Existing ops skill** `.claude/skills/deploy` drives the local stack — project ops tooling, not part of the `team-workflow` lifecycle. Leave it alone.

- **Architecture archetypes** (for `estimate` task breakdowns): `TODO — record the recurring
  kinds of change in this codebase as they emerge.`
- **UI automation quirks**: `TODO (record them the first time a ticket is browser-verified)`.
