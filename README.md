# demo — reference Spring Boot module

Reference implementation of the CSnx lightweight module stack (CSNX-13935).
End-to-end webapp: customer registration → login → landing page showing the
authenticated user's info.

## Stack

**Backend** — Java 21, Spring Boot 3.5, Maven, Spring Data JPA + Hibernate,
Spring Security + JWT, Flyway, Log4j 2, springdoc-openapi, MapStruct,
PostgreSQL, Testcontainers, JUnit 5 + Mockito + AssertJ.

**Frontend** — React 18 + TypeScript, Vite, shadcn-style UI primitives
(Tailwind), React Router, TanStack Query, react-hook-form + zod.

**Container** — backend Dockerfile (or `./mvnw spring-boot:build-image`);
frontend multi-stage → nginx; `docker-compose.yml` boots Postgres + backend +
frontend together.

## Layout

```
backend/        Maven Spring Boot project, Java 21
  src/main/java/za/co/csnx/demo/...
  src/main/resources/application.yml + db/migration/
  pom.xml, mvnw, Dockerfile
frontend/       Vite + React + TypeScript
  src/{pages,components/ui,lib}/
  package.json, vite.config.ts, Dockerfile, nginx.conf
docker-compose.yml
```

## Prerequisites

- **Local dev**: Java 21 (Temurin recommended), Node 20.19+/22+ (for Vite 5),
  Docker (for Postgres or full compose).
- **Containerized**: only Docker.

## Run locally (dev mode)

### 1. Start Postgres

```bash
docker run --rm -d --name demo-pg \
  -e POSTGRES_DB=demo -e POSTGRES_USER=demo -e POSTGRES_PASSWORD=demo \
  -p 5432:5432 postgres:16-alpine
```

### 2. Backend

```bash
cd backend
JAVA_HOME=<path-to-jdk21> ./mvnw spring-boot:run
```

- API: <http://localhost:8080/api/...>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Actuator health: <http://localhost:8080/actuator/health>

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

- App: <http://localhost:5173>
- Vite proxies `/api` → `http://localhost:8080`.

## Run end-to-end (Docker)

```bash
docker compose up --build
```

- Frontend: <http://localhost:8081>
- Backend: <http://localhost:8080>
- Postgres: localhost:5432 (demo / demo)

## Tests

```bash
cd backend
JAVA_HOME=<path-to-jdk21> ./mvnw test
```

Includes unit tests (`JwtServiceTest`, `AuthServiceTest`) and an integration
test (`DemoBackendApplicationTests`) that boots the full Spring context against
a Testcontainers Postgres instance.

## API

| Method | Path                  | Description                         | Auth |
|--------|-----------------------|-------------------------------------|------|
| POST   | `/api/auth/register`  | Create a customer                   | none |
| POST   | `/api/auth/login`     | Get access JWT + refresh cookie     | none |
| POST   | `/api/auth/refresh`   | Exchange refresh cookie for new JWT | none |
| POST   | `/api/auth/logout`    | Clear refresh cookie                | none |
| GET    | `/api/me`             | Authenticated customer's profile    | JWT  |

## Scaffolding a new module from this template

1. Copy this directory to `C:\software\projects\modules\<name>` and `git init` a
   new repo there.
2. Rename the Maven `artifactId` and Java package `za.co.csnx.demo` →
   `za.co.csnx.<name>`.
3. Update `application.yml` (database name, JWT issuer) and the Flyway baseline
   migration to your domain's tables.
4. Update `docker-compose.yml` service names and the frontend's `nginx.conf`
   `proxy_pass` target.
5. Replace the demo screens with module-specific ones — the auth foundation,
   `BaseEntity`, `BaseRepository`, `GlobalExceptionHandler` and shadcn primitives
   carry over unchanged.

## Future direction

The frontend will evolve into a metadata-driven UI engine equivalent to CSnx's
GWT engine (`MetadataManagerBean` + `DynamicFormView`). The current shadcn
primitives are intentionally simple so they can be reused as leaf renderers in
that engine. See `docs/plans/CSNX-13935-plan.md` (in the CSnx repo) for context.
