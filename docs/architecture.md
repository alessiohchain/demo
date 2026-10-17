# Demo — module architecture

The static picture of this repo: what lives where, its ports and env vars, and
the flows only demo runs. Everything the fleet shares is linked, not repeated:
container anatomy, base stack and the run template
([fleet.md](../../platform/docs/fleet.md)), sign-in
([relying-party-auth.md](../../platform/docs/relying-party-auth.md)), the
metadata engine ([engine.md](../../platform/docs/engine.md)), code conventions
incl. shadcn and theming ([module-conventions.md](../../platform/docs/module-conventions.md)),
the platform plus its consumers ([platform-architecture.md](../../platform/docs/platform-architecture.md)).

## Repo shape

```
backend/                 Spring Boot, za.co.csnx.demo.DemoBackendApplication → demo-backend jar
frontend/                Vite + React 19 + TS strict → nginx image
docker-compose.yml       postgres (wal2json build) + backend + frontend
infra/postgres-wal2json/ the 3-line postgres 16 + wal2json image for WAL CDC
infra/azure/, infra/gcp/ Bicep (Container Apps) and Terraform (Cloud Run) — runbook: docs/demo-ops.md
scripts/                 stack.ps1 / redeploy.ps1 (local stack), vendor-engine.ps1,
                         deploy-azure.ps1, deploy-infra-azure.ps1, deploy-gcp.ps1, db-proxy.ps1
audits/2026-05-26/       the template-era audit (frozen record)
```

## Backend packages (`za.co.csnx.demo`)

| Package | Holds |
|---|---|
| `service.activity` → `business.activity` → `repository` / `domain` | The worked-example screens' three layers (AGENTS.md §Screens) |
| `events` | `DemoEventConsumers.integrationEvents` (one consumer, dispatches by type) + the `int_company` / `int_vendor` mirror stores |
| `security`, `config` | `JwtAuthFilter` + `PlatformTokenVerifier`, `SecurityConfig`, CORS |
| `web` | Lookup / Metadata / Process / SessionBootstrap controllers; `TestCallController` + `TestEchoActivityService` (temporary, `csnx.test-services.enabled`) |
| `service`, `ai` | `AppUserSeeder` (local profile cache), `LookupService`; the smart-nav `DemoQueryLogStore` |

There is no backend `engine` package — the engine is the vendored
`csnx-engine-spring` / `csnx-engine-ai` artifacts.

## Auth

Relying party of the platform IdP, no auth endpoints of its own. Demo's
[per-module values](../../platform/docs/relying-party-auth.md#per-module-values):
SPA client / audience `demo-module`, module code `DEMO`, service client
`demo-service` + `DEMO_SERVICE_SECRET`; public paths = swagger, actuator
health/info and the temporary `/api/test/**`.

## Ports

| | Host | Notes |
|---|---|---|
| Backend | **8092** | container 8080; `server.port` defaults to 8092 outside compose |
| Frontend | **8085** | nginx :80; Vite dev server **5175** (proxies `/api` to :8092) |
| Postgres | **5435** | dev datasource default is `localhost:5435/demo` |
| RabbitMQ | 5672 | owned by the platform compose stack, reached via `host.docker.internal` |

Fleet registry: [fleet.md §Fleet ports](../../platform/docs/fleet.md#fleet-ports).

## Environment (compose)

- **Platform**: `PLATFORM_ISSUER`, `PLATFORM_JWKS_URI`, `PLATFORM_REGISTRY_URL`,
  `DEMO_SERVICE_SECRET`; frontend `PLATFORM_ISSUER` + `PORTAL_URL`.
- **Integration**: `CSNX_EVENTS_ENABLED`, `RABBITMQ_HOST`,
  `CSNX_SERVICE_PROCESS_ENABLED` (Channel-1 callee, so the platform's
  Integration Trace can read demo's ledgers), `CSNX_ENGINE_CDC_ENABLED` +
  `CSNX_SERVICE_CLIENT_ENABLED` + `SERVICE_TARGET_URL` +
  `CSNX_ENGINE_SERVICE_CLIENT_PLATFORM_BASE_URL` (the CDC config is fetched from
  the platform over Channel 1). All default on in compose, off in `application.yml`.
- **Smart-nav**: `GEMINI_API_KEY`, `APP_LLM_PROVIDER`, `SPRING_AI_MODEL_CHAT`.

## Module-only flows

- **WAL CDC producer**: the engine captures `demo.scwt_trader` off slot
  `csnx_cdc_demo` into the outbox as `demo.trader.changed` (consumed by pom);
  the capture row is seeded by `V20260922090000`, the config set lives on the
  platform (CDCC, `module_cd=DEMO`).
- **Event consumer**: `platform.company.changed` → `demo.int_company` and
  `pom.vendor.changed` → `demo.int_vendor` (consumer group `demo`).
  Walkthrough: [cdc-event-integration.md](../../platform/docs/cdc-event-integration.md).
- **Spring Batch** tables are created by the framework
  (`spring.batch.jdbc.initialize-schema: always`); the engine scheduler owns
  every launch.

## Images

- **Backend**: JRE 21 only — it COPYs a prebuilt `target/demo-backend-*.jar`
  (`scripts/redeploy.ps1` builds it); attaches the Application Insights agent
  only when `APPLICATIONINSIGHTS_CONNECTION_STRING` is set.
- **Frontend**: `npm ci` against GitHub Packages (the `gh_token` build secret),
  Vite build, nginx.
