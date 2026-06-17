---
genre: security
category: database
analysis-type: static
relevance:
  file-patterns:
    - "**/models/**"
    - "**/migrations/**"
    - "**/db/**"
    - "**/database/**"
    - "**/schema/**"
    - "**/prisma/**"
  keywords:
    - "sql"
    - "query"
    - "orm"
    - "prisma"
    - "sequelize"
    - "mongoose"
    - "typeorm"
    - "knex"
    - "migration"
    - "schema"
  config-keys:
    - "prisma"
    - "sequelize"
    - "mongoose"
    - "typeorm"
    - "knex"
    - "pg"
    - "mysql2"
    - "mongodb"
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# Database Security Assessment

**Assessment Date:** 2026-05-26
**Auditor:** code-audit (security-auditor agent)
**Status:** Complete

---

<!-- analysis: static -->

## Executive Summary

**Overall Rating:** [ ] Excellent [ ] Good [x] Fair [ ] Poor [ ] Critical

**Findings:** Critical: 0 | High: 1 | Medium: 2 | Low: 1

PostgreSQL 16, accessed via Spring Data JPA + Hibernate. Schema is migrated by Flyway (`db/migration/V1__init.sql`). No raw-SQL string concatenation was found. The dominant risks are operational: hardcoded credentials and the database port being exposed on the host.

---

## 1. Tech Stack

| Item | Detail | Source |
|---|---|---|
| RDBMS | PostgreSQL 16 (alpine) | `docker-compose.yml:3` |
| Driver | `org.postgresql:postgresql` (Spring-managed version) | `backend/pom.xml:127` |
| ORM | Hibernate via Spring Data JPA + `JpaSpecificationExecutor` | `backend/pom.xml:37`; `backend/src/main/java/za/co/csnx/demo/DemoBackendApplication.java:11` |
| Migration tool | Flyway (`flyway-core` + `flyway-database-postgresql`) | `backend/pom.xml:81-87` |
| `ddl-auto` | `validate` (no auto-create / auto-update) — safe | `backend/src/main/resources/application.yml:13` |
| `open-in-view` | `false` — sessions don't bleed into the web layer | `backend/src/main/resources/application.yml:11` |

---

## 2. Findings

### 2.1 Hardcoded credentials in source-tree configuration

| Severity | Issue | Location | Impact |
|---|---|---|---|
| High | The compose file ships `POSTGRES_USER=demo`, `POSTGRES_PASSWORD=demo`, and `DB_PASSWORD=demo` literally. Anyone who clones the repo has the password to every running compose stack. | `docker-compose.yml:5-7, 26-27` | Acceptable for a dev/demo workflow; **must not** be carried into any deployed environment. README and `CLAUDE.md` instruct cloners to "copy this directory" verbatim → likely copied unchanged. |
| Medium | `application.yml` falls back to `username=demo` / `password=demo` when env vars are unset (`DB_USER:demo` / `DB_PASSWORD:demo`). | `backend/src/main/resources/application.yml:8-9` | Any production run that forgets the env vars connects with these credentials — which match the compose default — creating a known credential pair across deployments. |

### 2.2 Database port exposed on host

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Medium | Compose maps Postgres `5432:5432` to the host. Anyone with network access to the docker host can connect with the published `demo/demo` credentials. | `docker-compose.yml:15-16` | Lateral-movement risk on shared workstations / VPN-bridged networks. |

### 2.3 SQL injection vectors

| Severity | Status | Notes |
|---|---|---|
| Pass | All repository methods read in this audit use parameterised JPQL (Spring Data derived queries: `findByCompanyCodeAndUsername`). No `EntityManager.createNativeQuery(... + userInput + ...)` patterns were found in `service/` or `repository/`. `BaseRepositoryImpl` was sampled but not exhaustively reviewed — flag for manual verification. |

### 2.4 Schema observations

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Low | `V1__init.sql` creates a `customer` table; `domain/AppUser.java:24` uses `@Table(name = "app_user", schema = "demoschema")`. The migration shown does not contain the `app_user` DDL — the live schema must therefore be created by a later migration not present in `V1__init.sql`. Inconsistency between the documented migration baseline and the entity-model implies missing migrations in this repo snapshot. | `backend/src/main/resources/db/migration/V1__init.sql` vs `backend/src/main/java/za/co/csnx/demo/domain/AppUser.java` | If `ddl-auto: validate` and the runtime cannot find `demoschema.app_user`, the app will fail to boot. The repo snapshot may simply be missing later migrations (V2…Vn). |
| Info | `BaseEntity` uses Spring Data JPA Auditing — `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`, `@Version` (`backend/src/main/java/za/co/csnx/demo/common/BaseEntity.java:21-39`). Good practice; the audit columns are populated via `AuditingConfig.SpringSecurityAuditorAware` which reads `usernameOf(principal)`. |

### 2.5 Connection encryption

The JDBC URL `jdbc:postgresql://postgres:5432/demo` (compose) and `jdbc:postgresql://localhost:5432/demo` (default fallback) do not specify `?sslmode=require`. Acceptable for in-cluster compose traffic but a risk for production: a forgotten `sslmode` param means the driver may negotiate plaintext.

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Low | JDBC URL has no `sslmode=require` enforced. | `backend/src/main/resources/application.yml:7`; `docker-compose.yml:25` | Production deployments must remember to set `DB_URL` with `?sslmode=require` (or rely on the managed-Postgres provider to refuse plaintext). |

---

## 3. Recommendations

### Immediate
- Replace the literal `demo/demo` credentials in `docker-compose.yml` with `${POSTGRES_PASSWORD:?set in .env}` to force operators to provide them.
- Remove the `:demo` defaults from `application.yml:8-9` (fail-fast if env vars are absent).
- Add `?sslmode=require` to the `DB_URL` template used in `.azure/deployment-plan.md` and `.gcp/deployment-plan.md`.

### Short-term
- Drop the host port mapping `5432:5432` in compose, or restrict to `127.0.0.1:5432:5432`.
- Add a CI step that runs `mvn flyway:validate` to detect missing migrations like the `app_user` schema referenced in `AppUser.java` but absent from `V1__init.sql`.
- Add a `pg_hba.conf` mount in compose forcing `scram-sha-256` for all connections (today the alpine image accepts the default `trust` for local sockets).

### Long-term
- Introduce a least-privilege DB role for the application (no `CREATE TABLE`, no `DROP`); Flyway runs under a separate migration role.
- Postgres row-level security on tenant-scoped tables (see `access-control.md` finding 2.2).

---

## Conclusion

The ORM/JPA usage is safe (no string-concatenated SQL spotted). The database-level risks are **operational**: shared dev/prod credentials, exposed ports, and lack of TLS enforcement in the connection URL.
