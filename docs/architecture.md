# Module architecture

How this Spring Boot + React reference module is wired together, end to
end. Read this before making cross-cutting changes; it's faster than
piecing it together from the code.

For the **rules** that flow from these choices (no Lombok, no Logback,
shadcn-via-CLI, etc.) see [`../CLAUDE.md`](../CLAUDE.md). For the
**rationale** behind each choice see [`./decisions.md`](./decisions.md).
For the **multi-module future** this template seeds, see
[`./platform-architecture.md`](./platform-architecture.md).

## Repo shape

One git repo, two deployables, one polyglot Claude Code session at the
root.

```
demo/
├── backend/             Spring Boot 3.5 / Java 21 / Maven
├── frontend/            Vite + React 18 + TS strict
├── docker-compose.yml   postgres + backend + frontend
├── CLAUDE.md            Conventions Claude must follow
└── docs/
    ├── architecture.md          (this file)
    ├── decisions.md             Why each stack choice was made
    └── platform-architecture.md Future multi-module shape
```

The backend and frontend are sized to be edited together — most
real changes touch DB → entity → DTO → controller → axios client →
form. Cloning to a sibling `modules/<name>` directory creates a new
module with the same shape.

## Backend

### Stack

| Layer | Choice |
|---|---|
| Language | Java 21 (LTS, records + pattern matching + `var` everywhere) |
| Framework | Spring Boot 3.5 (Spring 6, Jakarta EE) |
| Build | Maven via `./mvnw` |
| Persistence | Spring Data JPA + Hibernate, `JpaSpecificationExecutor` for filtering |
| Migrations | Flyway (`db/migration/V<version>__*.sql`; new migrations use a `V<yyyyMMddHHmmss>` timestamp — see [migrations.md](migrations.md)) |
| Validation | Jakarta Bean Validation (Hibernate Validator) |
| Auth | Spring Security stateless + JWT (jjwt) |
| DTO mapping | MapStruct (compile-time, `defaultComponentModel=spring`) |
| API docs | springdoc-openapi (`/v3/api-docs`, `/swagger-ui.html`) |
| Logging | SLF4J + Log4j 2 (Spring Boot starter swap; **not** Logback) |
| DB | PostgreSQL 16 (prod + dev), Testcontainers for tests (no H2) |
| Tests | JUnit 5 + Mockito + AssertJ + slice tests + Testcontainers |

### Package layout

```
za.co.csnx.demo
├── DemoBackendApplication       @SpringBootApplication entry point
├── common/      cross-cutting infra (no business logic)
│   ├── BaseEntity              audit fields + @Version (update_serial)
│   ├── BaseRepository          JpaRepository + JpaSpecificationExecutor + findByIdOrThrow
│   ├── BaseRepositoryImpl      backs findByIdOrThrow with the entity name
│   ├── BusinessException       single + multiple business messages, toast/dialog on the wire
│   ├── EntityNotFoundException 404 mapping
│   ├── GlobalExceptionHandler  @RestControllerAdvice → RFC 7807 ProblemDetail
│   └── MessageType             ERROR / WARNING / INFO enum
├── config/      Spring configuration beans
│   ├── SecurityConfig          filter chain, CORS, password encoder, AuthN manager
│   └── CorsProperties          @ConfigurationProperties for allowed origins
├── domain/      JPA entities (extend BaseEntity, hand-written getters/setters)
│   └── Customer
├── repository/  Spring Data interfaces (extend BaseRepository)
│   └── CustomerRepository
├── security/    Authentication plumbing
│   ├── JwtService              issue + parse access/refresh tokens, jjwt
│   ├── JwtAuthFilter           reads Authorization, sets SecurityContext
│   ├── JwtProperties           secret + TTLs from config
│   └── AuditorAware            feeds BaseEntity audit fields from SecurityContext
├── service/     Transactional business logic
│   ├── AuthService             register / login / refresh
│   └── CustomerService         CRUD + Specifications
└── web/         Controllers + DTOs (records) + MapStruct mappers
    ├── AuthController, MeController, DemoController
    └── dto/                    LoginRequest, RegisterRequest, AuthResponse, CustomerDto, ...
```

### Conventions worth knowing

- **Entities extend `BaseEntity`** — get `id`, `createdAt`, `createdBy`,
  `updatedAt`, `updatedBy`, and `@Version` (mapped to the `update_serial`
  column on every table). Audit fields are populated by `AuditorAware`
  reading the security context.
- **Repositories extend `BaseRepository<T, ID>`** — gets full
  `JpaRepository` + `JpaSpecificationExecutor` + a `findByIdOrThrow`
  that throws `EntityNotFoundException` with the entity's name baked in.
- **Services**: `@Transactional(readOnly = true)` at class level,
  `@Transactional` on writers. No DAO-level transactions, no facade
  beans.
- **DTOs are `record`s.** No Lombok anywhere — entities hand-write
  getters/setters; records cover the DTO case natively.
- **Errors flow through `GlobalExceptionHandler`** and come back as
  RFC 7807 `ProblemDetail` JSON. `BusinessException` (single + multi)
  is the dual-purpose carrier for user-facing messages with
  `MessageType` (`ERROR` / `WARNING` / `INFO`); the response carries a
  typed `messages` array the frontend renders as toast (1 message) or
  dialog (≥ 2).
- **No `@Slf4j`.** `private static final Logger log = LoggerFactory.getLogger(MyClass.class);`.

### Auth flow

```
POST /api/auth/login   → AuthService.login
                          ├─ AuthenticationManager.authenticate (BCrypt)
                          └─ JwtService.issueAccess/Refresh
                              ↓
Response: { accessToken, expiresIn }   + Set-Cookie: refreshToken=…; HttpOnly; Path=/api/auth

GET  /api/me            (Authorization: Bearer …)
                          ↓
JwtAuthFilter.doFilterInternal
   ├─ shouldNotFilter() skips /api/auth/**, /swagger-ui/**, /v3/api-docs/**, /actuator/health|info
   ├─ extracts Bearer token
   ├─ JwtService.parse(TYP_ACCESS) — signed + typ + exp checks
   └─ SecurityContextHolder.setAuthentication(...)
                          ↓
MeController returns the current user

POST /api/auth/refresh  (refreshToken cookie sent automatically)
                          ↓
AuthService.refresh trusts the signed refresh token; mints a new access token.
```

Access tokens live 1 h; refresh tokens 7 d. Refresh is HttpOnly,
SameSite, scoped to `/api/auth`. Symmetric HS256 today; the multi-
module future moves to RS256 + JWKS (see `platform-architecture.md`).

### Profiles & config

- `application.yml` — common defaults, dev-friendly logging.
- `application-dev.yml` — verbose SQL logging, demo endpoints
  (`@Profile({"dev","local"})` on `DemoController`).
- `application-prod.yml` — production-tightened logging, restricted
  CORS, secrets read from env.
- Secrets (`APP_SECURITY_JWT_SECRET`, `DB_PASSWORD`) come from env vars
  in every profile — never committed.

### Tests

- **Slice tests** (`@WebMvcTest`, `@DataJpaTest`) for fast loops.
- **`@SpringBootTest`** only for cross-cutting integration.
- **Testcontainers Postgres** instead of H2 — HQL/dialect divergence
  with H2 isn't worth the speed gain.

## Frontend

### Stack

| Layer | Choice |
|---|---|
| Build | Vite 5 |
| Framework | React 18 |
| Language | TypeScript strict (no `any`) |
| Routing | react-router-dom v6 (data routers) |
| Styling | Tailwind 3, semantic HSL CSS variables (light + dark) |
| UI primitives | shadcn/ui (Radix under the hood) — installed via CLI, not hand-rolled |
| Server state | TanStack Query (no API responses in component state) |
| Forms | react-hook-form + zod resolver |
| HTTP | axios with bearer + auto-refresh interceptor |
| Toasts/dialog | sonner (toast) + shadcn `Dialog` for ≥ 2 business messages |

### Source layout

```
frontend/src
├── main.tsx                   ReactDOM bootstrap
├── App.tsx                    QueryClientProvider → AuthProvider → RouterProvider + Toaster + BusinessMessageHost
├── router.tsx                 public/protected route wrappers
├── index.css                  Tailwind layers + semantic HSL tokens (--warning, --info, ...)
├── components/
│   └── ui/                    shadcn primitives (button, card, dialog, input, label) — added via CLI
├── lib/
│   ├── api.ts                 axios instance + typed endpoint wrappers (authApi, meApi, demoApi, ...)
│   ├── auth.tsx               useAuth() hook + AuthProvider; access token in memory
│   ├── business-messages.tsx  showBusinessMessages() + BusinessMessageHost (dialog + toast routing)
│   ├── queryClient.ts         shared TanStack QueryClient
│   └── utils.ts               cn() helper (clsx + tailwind-merge)
└── pages/
    ├── Landing.tsx            protected landing
    ├── Login.tsx              react-hook-form + zod
    └── Register.tsx           react-hook-form + zod
```

### How the pieces connect

- **Login** → `authApi.login` → axios POST → backend issues
  `{accessToken, ...}` + sets refresh cookie → `AuthProvider` stores
  the access token **in memory only** (no localStorage).
- **Subsequent requests** → axios request interceptor adds
  `Authorization: Bearer <accessToken>`.
- **401 response** → axios response interceptor calls
  `/api/auth/refresh` (refresh cookie travels automatically), updates
  the in-memory token, replays the original request once.
- **Server errors** → backend `GlobalExceptionHandler` returns
  RFC 7807 ProblemDetail with `messages: [{message, messageType}]`.
  axios passes them to `showBusinessMessages` which routes single
  messages to `sonner` toasts and multi-message lists to the
  `BusinessMessageHost` dialog.

### shadcn primitives — install, don't hand-roll

`frontend/components.json` is wired so `npx shadcn@latest add <name>`
drops the upstream component into `src/components/ui/` without
clobbering the customised `tailwind.config.js` / `index.css` (which
carry `warning` and `info` semantic tokens beyond the shadcn defaults).

**Rules of thumb (full list in `CLAUDE.md`):**
- Run from `frontend/`. `--overwrite --yes` when replacing.
- **Don't run `npx shadcn@latest init`** — it would clobber the customised CSS/Tailwind config.
- Patch upstream `space-y-*` / `space-x-*` to `gap-*` after every install.
- Don't import a primitive that hasn't been installed.

## Containers & deployment

`docker-compose.yml` defines the local stack: **postgres → backend →
frontend (nginx)**. Each service has a healthcheck; downstreams wait
for upstreams to be healthy.

```
┌────────────────────────────────────────────────────────────┐
│  Browser (host:8081)                                       │
│      │                                                     │
│      ▼                                                     │
│  ┌────────────────┐      proxy_pass /api/  ┌──────────────┐│
│  │ frontend       │ ───────────────────────▶│ backend     ││
│  │ nginx 1.27     │                        │ Spring Boot ││
│  │ /usr/share/    │                        │ JRE 21      ││
│  │   nginx/html   │                        │ port 8080   ││
│  │ (vite dist)    │                        └──────┬──────┘│
│  └────────────────┘                               │       │
│                                                   ▼       │
│                                             ┌───────────┐ │
│                                             │ postgres  │ │
│                                             │ 16-alpine │ │
│                                             │ port 5432 │ │
│                                             └───────────┘ │
└────────────────────────────────────────────────────────────┘
```

### Backend image (`backend/Dockerfile`)

Two-stage:

1. **build** — `eclipse-temurin:21-jdk-alpine` runs Maven, exploded
   the boot jar into `target/dependency/`.
2. **runtime** — `eclipse-temurin:21-jre-alpine`, non-root `app` user,
   copies the layered jar's `lib/`, `META-INF/`, and `classes/`
   separately so dependency layers cache independently from app code.

Entrypoint launches the main class directly off the classpath
(`exec java -cp /app:/app/lib/* za.co.csnx.demo.DemoBackendApplication`)
so `JAVA_OPTS` can be appended freely.

> Note: the comment at the top of `backend/Dockerfile` calls out that
> `./mvnw spring-boot:build-image` (Cloud Native Buildpacks) is the
> preferred long-term path. The Dockerfile exists for compose
> convenience and CI portability.

### Frontend image (`frontend/Dockerfile`)

Two-stage:

1. **build** — `node:22-alpine`, `npm ci`, `npm run build` produces
   `dist/`.
2. **runtime** — `nginx:1.27-alpine` serves `dist/` and proxies
   `/api/*` to the `backend` service. SPA fallback via
   `try_files $uri $uri/ /index.html;`.

Built-in `HEALTHCHECK` hits the root.

### Compose service contract

| Service | Image | Port (host) | Health gate |
|---|---|---|---|
| `postgres` | `postgres:16-alpine` | 5432 | `pg_isready` |
| `backend` | built from `./backend` | 8080 | `/actuator/health` |
| `frontend` | built from `./frontend` | 8081 → nginx :80 | `wget /` |

Backend env vars come from `docker-compose.yml`:
`SPRING_PROFILES_ACTIVE`, `DB_URL`, `DB_USER`, `DB_PASSWORD`,
`APP_SECURITY_JWT_SECRET`. Default profile is `dev` so the
`/api/demo/*` endpoints and verbose SQL logging are on; flip with
`SPRING_PROFILES_ACTIVE=prod docker compose up`.

Postgres data persists in the named volume `postgres-data`.

### Local quickstart

```powershell
# Full stack via compose (preferred)
docker compose up --build         # http://localhost:8081

# Or run pieces individually for fast inner loop:
# Postgres (one-shot — same db/user/pass as compose)
docker run --rm -d --name demo-pg `
  -e POSTGRES_DB=demo -e POSTGRES_USER=demo -e POSTGRES_PASSWORD=demo `
  -p 5432:5432 postgres:16-alpine

# Backend (Java 21 required)
cd backend ; ./mvnw spring-boot:run

# Frontend (Node 20.19+ / 22+)
cd frontend ; npm run dev          # http://localhost:5173

# Backend tests (Testcontainers)
cd backend ; ./mvnw test
```

When running backend + frontend natively, Vite's dev server proxies
`/api` to `http://localhost:8080` (configured in `vite.config.ts`).

### Cloud target

Container-first; the Dockerfiles + compose are meant to drop into
managed-container services on any major cloud. See the table in
`platform-architecture.md` for the recommended first targets per
cloud (Cloud Run / App Runner / Container Apps).

Backend horizontally scales freely today (stateless, JWT, no in-
memory state). Watch for: HikariCP × replicas exceeding the Postgres
connection cap, and `@Scheduled` jobs running on every replica
(reach for ShedLock if any are added).

## Where to look next

- **`CLAUDE.md`** — the conventions Claude (and humans) must follow
  when editing this repo.
- **`docs/decisions.md`** — short rationale per stack choice. Read
  before proposing a swap.
- **`docs/platform-architecture.md`** — the multi-module future this
  template seeds, including JWT migration to RS256 + JWKS, central
  scheduler, and frontend portal.
