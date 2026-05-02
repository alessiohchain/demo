# Architecture decisions

The **why** behind the conventions in `CLAUDE.md`. CLAUDE.md tells you the
rules; this file tells you what was on the table, what was rejected, and why
the choice landed where it did. Read this when a rule looks arguable, before a
new module is started, or before you propose changing a stack choice.

For the original ticket plan see `docs/plans/CSNX-13935-plan.md` in the
CSnx repo.

---

## 1. Goals & non-goals of the stack

**Goal:** a reusable lightweight stack for small containerized Spring Boot
apps that can run on the latest LTS JDK, free of CSnx's WebSphere 9 / Java 8
constraints. The first concrete consumer is the Order Sourcing module
(CSNX-13425). Future candidates: AppDep (CSNX-13427) and other small modules.

**Non-goals:** porting CSnx itself, replicating SmartGWT, supporting Java 8 /
WebSphere. New modules deploy as containers, not WARs.

The stack is **the template**; modules are siblings under
`C:\software\projects\modules\<name>`.

## 2. Java 21, Spring Boot 3.5, Maven

- **Java 21**: latest widely-adopted LTS (Sep 2023). Tooling, base images, and
  library support are stable. Java 25 (LTS, Sep 2025) was on the table but
  ecosystem still maturing as of early 2026 — not worth the friction for a
  template the team will reuse.
- **Spring Boot 3.5.x**: requires Jakarta EE 10 namespace and Java 17+. Brings
  ProblemDetail, structured logging primitives, observability via Micrometer,
  generation-aware GC defaults.
- **Maven**: chosen over Gradle because Spring Initializr defaults to it,
  config is declarative (m2e in Eclipse just works), and the team has
  decades of `pom.xml` muscle memory from CSnx. Gradle is faster on
  multi-module builds but lightweight modules don't feel that.

## 3. Persistence: Hibernate, not OpenJPA

CSnx uses **OpenJPA 3.0** because of TomEE/WebSphere lineage. Rejected for the
new stack:

- OpenJPA is in maintenance mode; no new feature work since 2018.
- Spring Data JPA's auto-config assumes Hibernate.
- Hibernate has the larger community, the active spec implementation, the
  better tooling (criteria API, second-level cache, multi-tenancy hooks if we
  ever need them).

Picked **Hibernate** as the JPA provider; the entity model still depends only
on the JPA spec (`jakarta.persistence.*`) so swapping providers is theoretically
possible but won't be needed.

## 4. PostgreSQL, not MySQL or DB2

- **DB2**: rejected for the new stack — heavy in containers, licensing for
  fresh instances is friction, and decoupling from CSnx's DB2 dependency is
  part of why this stack exists.
- **MySQL**: viable. Simpler operationally, fine for read-heavy CRUD.
- **PostgreSQL** (chosen): transactional DDL (atomic migrations), MVCC
  concurrency model, JSONB for evolving schemas, richer indexing (GIN, GiST,
  partial, BRIN). Hibernate's Postgres dialect is more featureful. Headroom
  for the kind of modules likely to follow.

## 5. Flyway, not MyBatis Migrations

CSnx uses **MyBatis Migrations** (CLI-driven). Rejected for the new stack
despite team familiarity:

- Flyway has a first-class Spring Boot starter — runs on app startup, no
  separate CLI step.
- Native `baseline-on-migrate`, `repair`, `clean`, info commands.
- Java-based migrations supported when SQL isn't enough.
- Industry default for Spring Boot projects — onboarding is faster.

Migration filenames: `V<n>__<snake_case>.sql`. **Never edit a released
migration**; add a new one.

## 6. Log4j 2, not Logback (and definitely not Log4j 1.x)

- **Log4j 1.x** (CSnx's choice for legacy reasons): EOL 2015, has CVEs.
  Off the table for anything new.
- **Logback** (Spring Boot default): perfectly fine, zero-config.
- **Log4j 2** (chosen): same family the team already thinks in, better async
  throughput, richer config DSL. Spring Boot ships `spring-boot-starter-log4j2`
  as a drop-in replacement (we exclude `spring-boot-starter-logging`).

## 7. Authentication: Spring Security + JWT + httpOnly refresh cookie

- **Stateless** — required for horizontal scaling / containers.
- **Access JWT** (1h, in-memory on the frontend) for `Authorization: Bearer`.
- **Refresh token** (7d) in an httpOnly cookie scoped to `/api/auth`. Cookie
  is httpOnly so XSS can't read it; SameSite + Secure flags will go on in
  prod profile.
- Axios interceptor on the frontend auto-refreshes on 401 once per request.

Rejected: **session cookie auth** (sticky sessions get awkward in load-balanced
containerized deploys), **OAuth2/Entra ID from day one** (heavier setup; the
JWT design here keeps that swap-in path open — see CSNX-13427's Entra ID work).

Open follow-ups (intentional): refresh-token rotation on logout/compromise;
rate limiting on `/api/auth/*` (Bucket4j or gateway-level).

## 8. No Lombok; records for DTOs, hand-written getters for entities

Initial version of this stack used Lombok (`@Getter @Setter @NoArgsConstructor`
on entities). **Removed** because:

- Eclipse needs a per-install Lombok agent — the dev's six Eclipse installs
  meant tracking which one was patched. High friction for two entity classes.
- Modern Java already covers most Lombok use cases:
  - `record` replaces `@Value` / `@Data` for immutable DTOs (we already use
    records for `RegisterRequest`, `LoginRequest`, `AuthResponse`,
    `CustomerDto`).
  - Spring 6 auto-injects on a single constructor → `@RequiredArgsConstructor`
    is rarely needed.
- JPA entities **cannot be records** (need a no-arg constructor, mutable state
  for proxies/lazy loading, JavaBean accessors for Jackson/MapStruct). So they
  must be classes — but a 4-field entity with hand-written getters/setters is
  ~30 lines of trivial boilerplate, not worth a build-time agent.

If a module's entity grows past 15 fields and the boilerplate becomes
genuinely painful, re-add Lombok **per module** — the stack is the template,
not the gospel.

## 9. MapStruct for DTO mapping

Compile-time, no reflection, no Lombok dependency (we removed the
`lombok-mapstruct-binding` annotation processor when Lombok left). Generated
mappers are visible in `target/generated-sources` for debugging.

Rejected: hand-written mappers (boilerplate at scale), ModelMapper (runtime
reflection, harder to debug, slower).

## 10. Frontend: React + TS + Vite + shadcn + TanStack Query + RHF/zod

- **React + TypeScript** — strict mode. No `any`.
- **Vite** over CRA (deprecated) and Next.js (overkill for SPA-only modules).
- **shadcn-style UI primitives** — vendored as source in `frontend/src/components/ui/`,
  not an npm dependency. Built on Radix primitives + Tailwind. Reasoning: each
  module owns its UI tokens / colors; the primitives are intentionally simple
  so a future metadata-driven engine can use them as leaf renderers without
  fighting an opinionated component library.
- **TanStack Query** for server state (cache, retries, devtools). No Redux.
- **react-hook-form + zod** for forms. zod schema is the runtime validator AND
  the TS type source.
- **Axios** for HTTP — interceptors for bearer / auto-refresh cleaner than
  hand-rolling on `fetch`.

Long-term: generate the TS API client from `/v3/api-docs` (springdoc) via
`openapi-typescript` so request/response types stay in sync. Hand-typed for
now — premature for one endpoint group.

## 11. Repo layout: one repo per module, backend/ and frontend/ folders

- **One repo per module** (sibling folders under `modules/`). Modules are
  independently deployable; bundling them in a monorepo would couple release
  cadence.
- **Within a module: backend/ and frontend/ folders, not separate repos.**
  Cross-cutting changes (DB → entity → DTO → API → form) hit both ends in
  the same PR. Two repos = drift risk, two PRs.
- Demo module lives at `C:\software\projects\modules\demo`. Future modules
  copy the demo as a starting point.

## 12. Containerization: Dockerfile for compose, buildpacks for prod images

- `backend/Dockerfile` (multi-stage Temurin 21 → JRE alpine) is what
  `docker compose up` uses. Predictable, no surprise dependencies.
- For pure prod images, `./mvnw spring-boot:build-image` (Cloud Native
  Buildpacks) is the default — gives a CVE-scanned, layered image without
  hand-tuning a Dockerfile.
- Frontend: multi-stage Node 22 build → nginx 1.27 alpine, with `/api`
  reverse-proxied to the `backend` service.

## 13. Testing: JUnit 5 + Mockito + AssertJ + Testcontainers (no H2)

- **JUnit 5 + Mockito**: defaults.
- **AssertJ** instead of Hamcrest matchers — fluent, readable, single import.
- **Testcontainers Postgres** for integration tests, not H2. H2's SQL dialect
  diverges enough from Postgres that "passes in tests, fails in prod" is a
  real risk. CI cost of spinning up a Postgres container is ~3-5 seconds.
- Slice tests (`@WebMvcTest`, `@DataJpaTest`) for the fast loop; reserve
  `@SpringBootTest` for genuinely cross-cutting integration (auth flow,
  Spring context boots cleanly with the full app).

## 14. Future direction: metadata-driven UI engine

Long-term, the React frontend will evolve into a metadata-driven engine
equivalent to CSnx's GWT engine (`MetadataManagerBean` +
`DynamicFormView` / `DynamicGridComponent`) — screens, grids, dropdowns
rendered from server-driven metadata rather than written per-feature.

Likely path: JSON Schema for forms + TanStack Table for grids, with a
`MetadataController` serving `/api/metadata/screen/{name}` and
`/api/metadata/lookup/{code}`. shadcn primitives are intentionally simple so a
future renderer can dispatch to them as leaf components.

**Demo screens stay hand-coded for now** — the trajectory shapes the choice
of primitives, not the immediate implementation. Don't bake business logic
into demo screens; keep them easy to replace.

## 15. MCP servers — what's wired and where

- **GitLab** — user-scope (`~/.claude.json`, with PAT). Available
  automatically in any project. Used for issues, MRs, repo ops on
  `gitlab.com/WorldwideChainStores/...`.
- **Atlassian (Jira / Confluence)** — repo-scoped via `.mcp.json` at the demo
  root (SSE URL only, no secrets). Future modules cloning the demo inherit it.
  Cloud ID: `32b38c14-f87d-48a0-a8f4-43d094f9b637` (worldwidechainstores).
- **jdbc-db2** — CSnx-only. The demo doesn't connect to DB2.
