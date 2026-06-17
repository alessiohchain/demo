---
genre: infrastructure
category: back-end
analysis-type: static
relevance:
  file-patterns:
    - "**/server/**"
    - "**/src/**"
    - "**/app/**"
    - "**/backend/**"
  keywords:
    - "server"
    - "middleware"
    - "controller"
    - "service"
    - "handler"
  config-keys:
    - "express"
    - "fastify"
    - "django"
    - "flask"
    - "spring-boot"
  always-include: true
severity-scale: "Critical|High|Medium|Low|Info"
---

# Back-End Infrastructure Maturity

**Assessment Date:** 2026-05-26
**Auditor:** code-audit (infrastructure-auditor agent)
**Status:** Complete

---

<!-- analysis: static -->

## Maturity score — **4 / 5 (Modern / Minor gaps)**

The Spring Boot 3.5 backend follows current idioms cleanly: records as DTOs, MapStruct compile-time mapping, JPA + Flyway, JWT bearer + refresh cookie, RFC 7807 error envelope, Testcontainers for integration tests. Maturity loses ground for the missing CI pipeline and the small but real "debug code committed" / "doc-vs-code drift" signals.

| Rubric level | Criteria | Match |
|---|---|---|
| 5 — Excellent | Industry-leading: hexagonal architecture, full observability, CI/CD, perfect test coverage | No |
| **4 — Modern** | **Modern stack, idiomatic patterns, decent tests, minor gaps** | **Yes** |
| 3 — Functional | Works but dated patterns or missing layers | No |

---

## 1. Technology

| Layer | Choice | Version | Source |
|---|---|---|---|
| Language | Java | 21 (LTS) | `backend/pom.xml:18` |
| Framework | Spring Boot | 3.5.14 | `backend/pom.xml:8` |
| HTTP | Spring MVC (servlet) | (BOM) | `backend/pom.xml:67` |
| Persistence | Spring Data JPA + Hibernate + `JpaSpecificationExecutor` | (BOM) | `backend/pom.xml:37`; `backend/src/main/java/za/co/csnx/demo/DemoBackendApplication.java:11` |
| Migrations | Flyway | (BOM) | `backend/pom.xml:81-87` |
| Validation | Bean Validation (Hibernate Validator) | (BOM) | `backend/pom.xml:57` |
| Security | Spring Security + jjwt | 0.12.6 | `backend/pom.xml:47, 102-117` |
| DTO mapping | MapStruct | 1.6.3 | `backend/pom.xml:97, 168-178` |
| API docs | springdoc-openapi | 2.8.13 | `backend/pom.xml:91` |
| Logging | SLF4J + Log4j 2 (Logback excluded everywhere) | (BOM) | `backend/pom.xml:77` plus exclusions at lines 28-33, 39-43, 49-53, 59-63, 69-73 |
| Build | Maven Wrapper (mvnw) | 3.x | `backend/mvnw`, `backend/mvnw.cmd` |
| Containers | Multi-stage Dockerfile, non-root user | — | `backend/Dockerfile:5-24` |

---

## 2. Architecture

```
web/                          (REST controllers — AuthController, LookupController, MetadataController, ProcessController)
service/                      (Transactional services)
repository/                   (Spring Data JPA interfaces)
domain/                       (JPA entities, all extending BaseEntity)
security/                     (JwtService, JwtAuthFilter, AppUserDetailsService)
config/                       (SecurityConfig, OpenApiConfig, CorsProperties)
common/                       (BaseEntity, BaseRepository, GlobalExceptionHandler, BusinessException, AuditingConfig)
web/dto/                      (records — LoginRequest, AuthResponse)
web/dto/engine/               (engine-layer DTOs)
```

**Strengths:**
- Clean separation of concerns. No DAO-helper antipatterns.
- `BaseEntity` (audit columns + `@Version`) is `@MappedSuperclass`; every entity inherits cleanly. `update_serial` column name aligns with the CSnx legacy schema (`CLAUDE.md:71-73`).
- `BaseRepository<T, ID>` adds `JpaSpecificationExecutor` + `findByIdOrThrow` (`CLAUDE.md:74-76`).
- `JpaAuditing` is wired via `AuditingConfig` with `SecurityContextHolder`-based `AuditorAware` — audit columns reflect the calling principal.
- Multi-stage Dockerfile, non-root `app` user, OCI-style layer split for warm builds (`Dockerfile:11-20`).

**Gaps:**
- No package-by-feature organisation (`auth`, `lookup`, `process` are split across `web/`, `service/`, `domain/`). For a reference module that other modules clone, package-by-feature would scale better.
- `BusinessException` carries multi-message semantics that overlap with `MethodArgumentNotValidException`; opinionated but documented in `GlobalExceptionHandler.java:36-39`.

---

## 3. Build & runtime

| Item | State | Source |
|---|---|---|
| Build tool | Maven Wrapper, reproducible builds | `backend/mvnw*`, `backend/pom.xml` |
| Dependency lock | Maven uses transitive resolution; `pom.xml` pins explicit versions for non-BOM deps | `backend/pom.xml:18-22` |
| Annotation processors | MapStruct via `maven-compiler-plugin` configuration | `backend/pom.xml:164-180` |
| `mvnw spring-boot:build-image` | Preferred per `Dockerfile:1-3` comment, but the custom Dockerfile is also provided | `backend/Dockerfile:1-3` |
| `ddl-auto` | `validate` — no auto-DDL | `backend/src/main/resources/application.yml:13` |
| `open-in-view` | `false` — no session-in-view antipattern | `backend/src/main/resources/application.yml:11` |
| Profiles | `dev` (default), `prod` (override) | `backend/src/main/resources/application-{dev,prod}.yml` |
| Hot reload | `spring-boot-devtools` included as `runtime`+`optional` | `backend/pom.xml:120-124` |

**Strengths:**
- `ddl-auto: validate` + Flyway = explicit schema control.
- `open-in-view: false` is the modern choice (default was `true` until Spring Boot 2; opting out avoids the lazy-loading-from-view footgun).
- Profile structure is clean.

**Gaps:**
- The custom Dockerfile copies `BOOT-INF/lib` directly (line 18-20). If `spring-boot-devtools` ends up in `BOOT-INF/lib` (it shouldn't — `<optional>true</optional>` should exclude it from the repackaged jar, but the *original* fat-jar before unpacking is what `jar -xf` reads), the production image will carry it. **Verify by inspection of a built image**.

---

## 4. Error handling

`@RestControllerAdvice` at `backend/src/main/java/za/co/csnx/demo/common/GlobalExceptionHandler.java`:
- Maps `BusinessException` → 422 (single) / 422 (multiple) with `kind: business` / `kind: multiple`.
- Maps `EntityNotFoundException` → 404.
- Maps `MethodArgumentNotValidException` → 400 with structured `errors[]` array.
- Maps `BadCredentialsException` → 422 (deliberate, with comment explaining the engine's interceptor design).
- Catch-all `Exception` → 500 with `kind: internal` and detail "Unexpected error — see server logs" (no stack leak).

**Strengths:** uniform `kind`-discriminated envelope; comments explain the design choices (lines 18-35, 84-90).

**Gaps:** see security `back-end.md` for the verbose-error-detail findings.

---

## 5. Testing

| Item | Coverage |
|---|---|
| Unit tests | `JwtServiceTest.java` (3 tests covering issue/parse/tamper-reject) |
| Application context test | `DemoBackendApplicationTests.java` (boots the full context) |
| Testcontainers | `TestcontainersConfiguration.java` boots a Postgres container per test class |
| Test runner | `TestDemoBackendApplication.java` |
| Coverage tool | None configured (no JaCoCo plugin in `pom.xml`) |

**Strengths:** real Postgres via Testcontainers — no H2 dialect divergence.

**Gaps:** only 4 test files for ~55 production Java files. No coverage gate.

---

## 6. Findings

| Severity | Issue | Location | Recommendation |
|---|---|---|---|
| Medium | Test coverage is sparse — 4 test files for the entire backend. | `backend/src/test/java/za/co/csnx/demo/` | Add slice tests (`@WebMvcTest` for each controller, `@DataJpaTest` for each repository) per the convention `CLAUDE.md:88` describes. |
| Medium | No CI pipeline runs tests on push. The `mvnw test` step exists locally but never executes in a gating workflow. | (no `.github/workflows/`, no `.gitlab-ci.yml`) | Add a GitHub Actions workflow that runs `./mvnw verify` + Testcontainers on push and PR. |
| Low | `DemoBackendApplication.java:16` ships `System.out.println("fgfgfggf");` — committed debug code reduces maturity perception. | `backend/src/main/java/za/co/csnx/demo/DemoBackendApplication.java:16` | Delete. |
| Low | Doc drift: README documents `/api/auth/register` (line 96) absent from `AuthController.java`. | `README.md:96` | Regenerate docs from OpenAPI, or remove the stale row. |
| Info | No `JaCoCo` coverage plugin; no enforced coverage gate. | `backend/pom.xml` | Add `org.jacoco:jacoco-maven-plugin` with a per-package threshold. |

---

## Maturity verdict — **4**

Modern stack, idiomatic Spring Boot 3 conventions, clean error envelope, multi-stage Docker, but test breadth + CI gating + a couple of "debug code committed" signals keep it from Level 5.
