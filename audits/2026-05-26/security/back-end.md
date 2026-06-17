---
genre: security
category: back-end
analysis-type: static
relevance:
  file-patterns:
    - "**/server/**"
    - "**/src/**"
    - "**/app/**"
    - "**/backend/**"
    - "**/cmd/**"
  keywords:
    - "server"
    - "middleware"
    - "controller"
    - "service"
    - "handler"
    - "injection"
    - "sanitize"
    - "validate"
  config-keys:
    - "express"
    - "fastify"
    - "django"
    - "flask"
    - "spring-boot"
    - "gin-gonic"
    - "@nestjs/core"
  always-include: true
severity-scale: "Critical|High|Medium|Low|Info"
---

# Back-End Security Assessment

**Assessment Date:** 2026-05-26
**Auditor:** code-audit (security-auditor agent)
**Status:** Complete

---

<!-- analysis: static -->

## Executive Summary

**Overall Rating:** [ ] Excellent [ ] Good [x] Fair [ ] Poor [ ] Critical

**Findings:** Critical: 0 | High: 0 | Medium: 3 | Low: 2

Spring Boot 3.5 with Spring Security, JPA, Validation. Architecturally clean (DTOs, MapStruct, RFC 7807 error envelope). Notable issues are input-validation gaps, debug-leftover code in a production class, and verbose logging in dev that risks leaking PII.

---

## 1. Tech Stack

| Layer | Version | Source |
|---|---|---|
| Spring Boot | 3.5.14 | `backend/pom.xml:8` |
| Java | 21 | `backend/pom.xml:18` |
| Spring Data JPA + Hibernate | (managed by Spring Boot) | `backend/pom.xml:37` |
| Bean Validation (Hibernate Validator) | (managed by Spring Boot) | `backend/pom.xml:57` |
| JJWT | 0.12.6 | `backend/pom.xml:21` |
| Log4j 2 (replacing Logback) | (managed by Spring Boot) | `backend/pom.xml:77`; `backend/src/main/resources/log4j2-spring.xml` |

---

## 2. Findings

### 2.1 Debug println left in production entrypoint

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Medium | `System.out.println("fgfgfggf");` is committed in the application bootstrap. Runs on every startup. Bypasses the Log4j 2 pipeline so it is unstructured, untimestamped, and unredactable. | `backend/src/main/java/za/co/csnx/demo/DemoBackendApplication.java:16` | Operational hygiene: marks the application as not having a clean review pipeline; mild log-noise / log-injection signal that other prints may exist. |

### 2.2 Input validation is minimal

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Medium | The `LoginRequest` DTO has `@NotBlank` on `email`/`password` and **no** constraint on `companyCode`. The login flow normalises `companyCode` with `trim().toUpperCase()` (`AuthService.java:65`) but accepts any length / any characters. | `backend/src/main/java/za/co/csnx/demo/web/dto/LoginRequest.java:14-20`; `backend/src/main/java/za/co/csnx/demo/service/AuthService.java:60-66` | Storage abuse via oversize values; potential downstream issues if `companyCode` is composed into log lines or queries (the principal is `"{companyCode}|{username}"` — a `|`-bearing companyCode could shift the boundary). |
| Medium | The `principalOf(companyCode, username)` composition splits on the first `|` (`AppUserDetailsService.java:33`). A malicious username containing `|` would not be misparsed (separator finds the first one) but a malicious `companyCode` containing `|` would split incorrectly and could elevate to an unintended account. | `backend/src/main/java/za/co/csnx/demo/security/AppUserDetailsService.java:23,33,50` | Boundary-confusion risk. Add a regex constraint on `companyCode` (`[A-Z0-9]{1,8}` is implied by the column `cpy_cd VARCHAR(8)`) at the DTO level. |

### 2.3 SQL & ORM safety

| Severity | Status | Notes |
|---|---|---|
| Pass | All repository queries observed use Spring Data JPA derived queries or `Specification` predicates (`AppUserRepository.findByCompanyCodeAndUsername`). No string concatenation of user input into JPQL/SQL was found in `repository/` or `service/`. `BaseRepositoryImpl` exists but was not exhaustively reviewed. |

### 2.4 Error-handler verbosity

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Low | `application.yml:26-27` sets `server.error.include-message: always` and `include-binding-errors: always` globally. `GlobalExceptionHandler` returns raw exception messages in `detail` for `IllegalArgumentException` (line 100) and `EntityNotFoundException` (line 53). | `backend/src/main/resources/application.yml:25-27`; `backend/src/main/java/za/co/csnx/demo/common/GlobalExceptionHandler.java:53, 100` | Internal exception strings leak to API consumers (info disclosure). |

### 2.5 Logging hygiene

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Low | `application-dev.yml:5-6` enables `org.hibernate.SQL: DEBUG` and `org.hibernate.orm.jdbc.bind: TRACE`. The `bind: TRACE` setting logs **prepared-statement bind parameters** — for a login query, that's the username (and any other queryable PII). Defaults to dev profile only, but `docker-compose.yml:24` defaults to `SPRING_PROFILES_ACTIVE=dev` in compose, so any compose-based "production-ish" run leaks bind values. | `backend/src/main/resources/application-dev.yml:5-6`; `docker-compose.yml:23-24` | PII leak into stdout logs in any compose-default run. |

### 2.6 Generic exception handler shape

The catch-all `@ExceptionHandler(Exception.class)` returns `kind: "internal"` with detail `"Unexpected error — see server logs"` (line 107) and logs the stack via `log.error("Unhandled exception", ex)` (line 105). Good — server-side detail is preserved without leaking to the client.

---

## 3. Recommendations

### Immediate
- Delete `DemoBackendApplication.java:16` (`System.out.println("fgfgfggf");`).
- Switch `docker-compose.yml:24` default to `prod` (or to a `compose` profile that doesn't log bind values).
- Set `server.error.include-message: never` in `application-prod.yml`; keep `always` in dev only.

### Short-term
- Tighten DTO validation:
  - Add `@Pattern(regexp = "[A-Z0-9]{1,8}")` to `LoginRequest.companyCode`.
  - Add `@Size(min=1, max=255)` to email and `@Size(min=1, max=128)` to password.
- Replace the `|` delimiter in `principalOf(...)` with a non-printable separator (`` US, "Unit Separator") so user-controlled bytes cannot match it.

### Long-term
- Add a custom `AccessLogFilter` (Log4j 2 layout) that strips bearer tokens / Set-Cookie / sensitive headers before they hit stdout.

---

## Conclusion

The Spring Boot backend follows modern conventions (records as DTOs, MapStruct mapping, `@ControllerAdvice` error envelope). The standout issues are operational: debug code committed, verbose dev logging that overlaps with the default-compose profile, and weak DTO validation on the multi-company principal composition.
