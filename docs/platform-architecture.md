# Platform architecture

## Today — demo is the single-module template

Right now there is one module: **demo**. It's a Spring Boot 3.5 + React +
Postgres app, three independently built containers (`docker-compose.yml`),
self-contained — no shared platform service, no cross-module API calls, no
central scheduler. Every future module clones this directory.

What the template gives you out of the box:

- **One git repo, one Claude Code session at the root** — polyglot Java +
  TypeScript. Cross-cutting changes (DB → entity → DTO → API → form) are
  routine; the session sees both ends.
- **Three-container deploy** — Postgres → Spring Boot fat-jar → nginx
  serving the Vite build. Each layer scales / redeploys on its own.
- **Authentication baked in** — Spring Security + JWT access + refresh
  cookie, `JwtAuthFilter`, role-aware route wrappers in the React shell.
- **Metadata-driven UI engine** — workflows authored as JSON under
  `backend/src/main/resources/screens/<workflow>.json`, leaf-rendered by
  shadcn primitives. Two seeded screen families (RPTM + COSF/CSFD)
  demonstrate single-grid CRUD and master-detail.
- **Activity-service pattern** — `AbstractCrudActivityService<T, ID>` owns
  the boilerplate; a new screen is a JSON file + a 60-line activity. See
  [activity-services.md](activity-services.md).
- **CSnx-aligned data shape** — the `scct_*` / `scwt_*` tables, `cpy_cd`,
  `maint_*`, `update_serial` columns, BaseEntity inheritance chain
  matching CSnx's `MaintainableTranUserBaseEntity`. Porting a CSnx
  activity into demo is a column-name translation, not a re-design.
  See [dao-patterns.md](dao-patterns.md) and
  [migrations.md](migrations.md).

The full single-module wiring is in [architecture.md](architecture.md).
This doc only covers cross-module direction.

## Cloning to a new module

Copy `C:\software\projects\modules\demo` to
`C:\software\projects\modules\<name>` and:

1. `git init` a fresh repo there.
2. Rename Maven `artifactId` and Java package `za.co.csnx.demo` →
   `za.co.csnx.<name>`.
3. Update `application.yml` — database name, JWT issuer, schema name
   (`demo` → `<name>`).
4. Replace the Flyway baseline migrations with the new domain's tables.
   The auth foundation (V1, V5), `BaseEntity` hierarchy, repository
   contracts, engine wiring all carry over unchanged. Write **new**
   migrations with a `V<yyyyMMddHHmmss>` timestamp version (UTC at
   authoring time), not the next sequential integer, so concurrently
   authored branches can't collide on the same version — see
   [migrations.md](migrations.md).
5. Replace the seeded screens under `backend/src/main/resources/screens/`
   with module-specific ones. The engine, activity-service base class,
   and shadcn primitives carry over unchanged.
6. Update `docker-compose.yml` service names and the frontend's
   `nginx.conf` `proxy_pass` target if you change ports.

That's it. The template's job is to stay copyable; we add to it cautiously
because everything we add is something every future module also inherits.

---

## Future direction (sketch — not decided yet)

The rest of this doc captures early-design conversation about what happens
when there are 3-10 modules. Nothing here is committed; it's a sketchpad
to revisit when the second module gets built.

### Target shape

A small constellation of Spring Boot modules (~3-10), each cloned from
this template, plus a central **platform/admin module** that ties them
together. "Between a monolith and microservices" — chunky deployables,
not a microservice swarm. Eventually CSnx itself gets broken into modules
of similar shape.

**Module taxonomy:** "module" always means a **deployable module** — a
Spring Boot app cloned from `modules/demo`. Each is its own image, its
own deploy, its own DB schema (or DB).

**Not using Spring Modulith.** Internal package structure stays plain —
`domain` / `service` / `web` / `repository` per the existing template.
Boundaries enforced by code review + CLAUDE.md, not ArchUnit / Modulith.

### Proposed pieces

1. **Central platform/admin module** — one Spring Boot app. Owns user
   registration, authentication, RBAC, module catalog, scheduled jobs
   across all modules, admin/portal UI. Issues JWTs that every business
   module trusts.

2. **Business modules** — N Spring Boot apps cloned from demo. Validate
   JWTs from the platform (RS256 / JWKS — **not** the current symmetric
   HS256 secret). Pull a small `csnx-platform-client` Maven artifact for
   JWT filter, audit, common DTOs. Own their own data — never read each
   other's databases; if they need each other's data, they call APIs.

3. **Frontend portal** — single React shell. Fetches "my modules" from
   the platform after login, renders the menu, federates module UIs via
   **iframe** initially (boring, works). Module federation / micro-
   frontends only if the UX warrants it later.

4. **API gateway** — optional. One entry point routing `/portal/*` →
   platform, `/m/<module>/*` → that module. Defer until there's a
   concrete reason (CORS sprawl, single TLS surface, central rate
   limiting).

### Why not full microservices

Ops cost (deploys, observability, distributed tracing, cross-service
consistency) is real and not free. ~3-10 deployables doesn't need it.
Each deployable is chunky internally; conventions + the template's
package layout keep the inside organised.

### Decisions to make before module #2

- **JWT signing**: move to RS256 with JWKS endpoint on the platform.
  Don't copy a shared HS256 secret to every module — path of regret.
- **Service-to-service auth**: short-lived service tokens issued by the
  platform, signed with the same key, with a `service` claim and a
  `module` audience. Module JWT filter accepts both user and service
  tokens.
- **Database per module vs schema per module**: currently `demo`
  in a shared `demo` DB. For prod, lean toward DB-per-module to keep
  blast radius small.
- **Postgres connection pooler**: once any backend scales beyond 1-2
  replicas, drop a pooler in front (Cloud SQL built-in / RDS Proxy /
  Azure PgBouncer). Connection count is the first scaling wall, not CPU.

### Central scheduler — agreed direction

- Quartz with JDBC job store, lives in the platform module.
- Scheduler owns *when*. Module owns *what*. Each job triggers a REST
  call to the relevant module's endpoint.
- Admin UI lists/filters jobs by module (rows in `qrtz_*` tables).
- Open items (auth on the scheduler → module call, idempotency via
  `jobRunId` header, async pattern for long-running jobs, failure /
  retry policy, hot-path coupling caveat where platform DB outage
  stops scheduled work everywhere) all deferred until the central
  module is being built.

### Cloud deployment shape (rough sketch)

Container-first; current `Dockerfile`s + compose are the starting point.
Recommended first managed-container target on each cloud:

| Piece | GCP | AWS | Azure |
|---|---|---|---|
| Backend container | Cloud Run | App Runner / ECS Fargate | Container Apps |
| Frontend | GCS + Cloud CDN | S3 + CloudFront | Static Web Apps |
| Postgres | Cloud SQL | RDS PostgreSQL | Postgres Flexible Server |
| Secrets | Secret Manager | Secrets Manager | Key Vault |

- Spring Boot cold start is the main pain on scale-to-zero services.
  Buildpacks (`spring-boot:build-image`) or GraalVM native if it
  becomes a real complaint.
- Backend horizontally scales freely today (stateless, JWT, no in-memory
  state). Watch for: Flyway lock contention on simultaneous boot
  (Flyway handles it), `@Scheduled` jobs running on every replica (use
  ShedLock if any are added per-module), HikariCP × replicas exceeding
  Postgres connection cap.

### Things deliberately NOT decided yet

- Final auth provider (build vs. Keycloak / Auth0 / Cognito).
- Frontend federation strategy beyond iframe v1.
- Whether the platform DB is shared with any module or strictly its own.
- Inter-module API style (REST vs. async events). Default to REST until
  there's a reason to pull in a broker.
- API gateway product if/when one is added.

### Source

Working notes from a 2026-05-02 conversation about scaling, cloud
deployment, and module strategy. Treat as seed; flesh out per-section
in `decisions.md` when each concrete choice is made.
