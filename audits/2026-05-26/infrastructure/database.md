---
genre: infrastructure
category: database
analysis-type: static
relevance:
  file-patterns:
    - "**/models/**"
    - "**/migrations/**"
    - "**/db/**"
    - "**/prisma/**"
  keywords:
    - "sql"
    - "query"
    - "orm"
    - "prisma"
    - "sequelize"
    - "mongoose"
    - "typeorm"
  config-keys:
    - "prisma"
    - "sequelize"
    - "mongoose"
    - "typeorm"
    - "knex"
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# Database Infrastructure Maturity

**Assessment Date:** 2026-05-26
**Status:** Complete

---

<!-- analysis: static -->

## Maturity score — **4 / 5 (Modern)**

Postgres 16 via Spring Data JPA, Hibernate, Flyway. `ddl-auto: validate` (no auto-DDL), `open-in-view: false` (no session-in-view), Testcontainers for integration tests, audit columns via `BaseEntity` + `JpaAuditing`. Loses a level for the schema mismatch noted below (looks like a missing migration in this snapshot) and absence of connection-pool / read-replica configuration.

---

## 1. Stack

| Item | Detail | Source |
|---|---|---|
| RDBMS | PostgreSQL 16 (alpine in compose) | `docker-compose.yml:3` |
| Driver | `org.postgresql:postgresql` (runtime scope) | `backend/pom.xml:127` |
| ORM | Hibernate via Spring Data JPA + `JpaSpecificationExecutor` | `backend/pom.xml:37`; `backend/src/main/java/za/co/csnx/demo/DemoBackendApplication.java:11` |
| Migrations | Flyway 9.x (BOM-managed) | `backend/pom.xml:81-87`; `backend/src/main/resources/db/migration/V1__init.sql` |
| Test DB | Testcontainers Postgres | `backend/pom.xml:147-154`; `backend/src/test/java/za/co/csnx/demo/TestcontainersConfiguration.java` |
| Connection pool | HikariCP (Spring Boot default) | (not explicitly tuned) |
| `ddl-auto` | `validate` | `backend/src/main/resources/application.yml:13` |
| `open-in-view` | `false` | `backend/src/main/resources/application.yml:11` |
| Auditing | `JpaAuditing` + `BaseEntity` audit columns | `backend/src/main/java/za/co/csnx/demo/common/AuditingConfig.java:13-27` |
| Optimistic locking | `@Version` field mapped to `update_serial` column | `backend/src/main/java/za/co/csnx/demo/common/BaseEntity.java:37-39` |

---

## 2. Schema management

- Flyway migrations live in `backend/src/main/resources/db/migration/V<n>__<snake_case>.sql` per the convention in `CLAUDE.md:90-92`.
- `V1__init.sql` creates `demoschema.customer` (the original demo schema), but the active domain (`AppUser` mapped to `demoschema.app_user`, plus `Company`, `LookupValue`, `MenuItem`, `ReportText`, `ScreenMetadata`) is **not** created by `V1`. There must be later migrations (`V2…Vn`) that this snapshot lacks — otherwise the app cannot boot under `ddl-auto: validate`.
- `baseline-on-migrate: true` (`application.yml:19`) means an empty DB gets a baseline marker without applying migrations — fine for greenfield.

| Severity | Issue | Location | Recommendation |
|---|---|---|---|
| High | The snapshot's `db/migration/` contains only `V1__init.sql`, which creates `customer`. The active domain references `demoschema.app_user`, `company`, `lookup_value`, `menu_item`, `report_text`, `screen_metadata` — none of which appear in `V1`. The repo is incomplete (later migrations are missing) or the active schema is created out-of-band. | `backend/src/main/resources/db/migration/V1__init.sql`; `backend/src/main/java/za/co/csnx/demo/domain/AppUser.java:24` | Locate and commit the missing migrations; add a CI step that runs `mvn flyway:validate` against a fresh container to catch this in future. |

---

## 3. Repository layer

- All repositories extend `BaseRepository<T, ID>` (`CLAUDE.md:74-76`), which itself extends `JpaRepository` + `JpaSpecificationExecutor` and adds `findByIdOrThrow`.
- Sampled: `AppUserRepository.findByCompanyCodeAndUsername(...)` — derived query, parameterised.
- `BaseRepositoryImpl` is wired via `@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl.class)` at `DemoBackendApplication.java:11`.
- No native SQL strings found in the read sample.

---

## 4. Connection / pool / replicas

| Item | State |
|---|---|
| Pool max size | Default (Spring Boot Hikari default = 10) |
| Pool timeouts | Default |
| Read replica routing | Not configured |
| Statement cache | Hibernate default |
| Lazy fetching defaults | Standard JPA |

Single-tier deployment — no read-replica routing. Acceptable for the demo workload.

---

## 5. Findings

| Severity | Issue | Location | Recommendation |
|---|---|---|---|
| High | Missing migrations (see §2 above). | `backend/src/main/resources/db/migration/` | Add the rest of the V-series and a CI gate. |
| Low | No explicit HikariCP tuning. Defaults are sane for small deployments, but a 10-connection ceiling will surprise operators when the actuator metrics endpoint goes live. | `backend/src/main/resources/application.yml` | Tune `spring.datasource.hikari.maximum-pool-size` per environment. |
| Low | No JDBC TLS enforcement (`?sslmode=require` missing from `DB_URL` template). | `backend/src/main/resources/application.yml:7`; `docker-compose.yml:25` | Add `?sslmode=require` to the prod-deployment URL templates. |
| Info | No DB-level access logging or query auditing beyond JPA's own log channels. | — | Acceptable for the workload. |

---

## 6. Strengths

- `ddl-auto: validate` plus Flyway is the correct combination.
- `JpaAuditing` populates `created_by` / `updated_by` from the security context.
- Optimistic locking column (`update_serial`) is centralised on `BaseEntity` so every entity inherits the same convention.
- Testcontainers-backed tests give realistic dialect behaviour.

---

## Maturity verdict — **4**

Strong choices, clean migration discipline (in principle — the snapshot's missing migrations are a CI gap, not a design gap), excellent audit-column wiring. Tuning + replica routing + the missing-migration discovery hold it at 4.
