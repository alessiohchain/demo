# demo — reference Spring Boot module

Reference implementation of the CSnx lightweight module stack (CSNX-13935).
End-to-end webapp: customer registration → login → landing page showing the
authenticated user's info.

## Stack

**Backend** — Java 21, Spring Boot 3.5, Maven, Spring Data JPA + Hibernate,
Spring Security + JWT, Flyway, Log4j 2, springdoc-openapi, MapStruct,
PostgreSQL, Testcontainers, JUnit 5 + Mockito + AssertJ.

**Frontend** — React 18 + TypeScript, Vite, shadcn/ui (Tailwind),
React Router, TanStack Query, react-hook-form + zod.

**Container** — backend via Spring Boot buildpacks; frontend multi-stage →
nginx; `docker-compose.yml` brings up Postgres + backend + frontend.

## Layout

```
backend/    Maven Spring Boot project
frontend/   Vite + React + TypeScript
docker-compose.yml
```

## Run locally

See `backend/README.md` and `frontend/README.md` (added during scaffolding).

## Scaffolding a new module

This repo is the template for future modules at `C:\software\projects\modules\<name>`.
Copy the folder, rename packages and database name, and update `docker-compose.yml`.
