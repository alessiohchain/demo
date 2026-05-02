# CLAUDE.md

Reference Spring Boot module stack (CSNX-13935). Polyglot project: Java
backend + React frontend in one repo. The first concrete consumer; future
modules at `C:\software\projects\modules\<name>` clone this template.

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

# All-in-one container
docker compose up --build   # http://localhost:8081
```

## Conventions — backend

- **No Lombok.** Hand-write getters/setters on entities (only `BaseEntity` and
  `Customer` so far). DTOs are `record`s — modern Java covers that case.
- **Entities extend `BaseEntity`** (audit fields + `@Version`). Implements
  `Serializable`; declare `private static final long serialVersionUID = 1L;`.
- **Repositories extend `BaseRepository<T, ID>`** — gets `JpaRepository` +
  `JpaSpecificationExecutor` + `findByIdOrThrow`.
- **Services**: `@Transactional(readOnly = true)` at class level, `@Transactional`
  on writers. No DAO-level transactions. No `DAOHelperBean`-style facades.
- **Controllers**: validate with `@Valid`, map entities → DTOs via MapStruct,
  return `ResponseEntity<DTO>` or `DTO`. Errors flow through
  `GlobalExceptionHandler` → RFC 7807 `ProblemDetail`.
- **Auth**: `JwtAuthFilter` reads `Authorization: Bearer …`, sets
  `SecurityContext`. Refresh token lives in `refreshToken` httpOnly cookie
  scoped to `/api/auth`. `AuditorAware` reads username from the security
  context for audit fields.
- **Migrations**: `V<n>__<snake_case>.sql` in `db/migration/`. Don't edit a
  released migration — write a new one.
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

## Future direction — metadata-driven UI

Long-term aim: replicate CSnx's GWT engine (`MetadataManagerBean` +
`DynamicFormView`/`DynamicGridComponent`) on this stack. shadcn primitives are
intentionally simple so a future renderer can dispatch to them. Likely path:
JSON Schema for forms + TanStack Table for grids, served from
`/api/metadata/screen/{name}` and `/api/metadata/lookup/{code}`. Demo screens
stay hand-coded for now — keep business logic out of them so they're easy to
replace later.

## Plan / ticket

Origin plan: `docs/plans/CSNX-13935-plan.md` in the CSnx repo (branch
`CSNX-13935`). Ticket: <https://worldwidechainstores.atlassian.net/browse/CSNX-13935>.
