---
genre: security
category: secure-logging
analysis-type: static
relevance:
  file-patterns:
    - "**/logger/**"
    - "**/logging/**"
    - "**/log/**"
  keywords:
    - "log"
    - "logger"
    - "winston"
    - "pino"
    - "bunyan"
    - "console.log"
    - "sentry"
    - "datadog"
  config-keys:
    - "winston"
    - "pino"
    - "bunyan"
    - "morgan"
    - "@sentry/node"
    - "log4j"
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# Secure Logging Assessment

**Assessment Date:** 2026-05-26
**Auditor:** code-audit (security-auditor agent)
**Status:** Complete

---

<!-- analysis: static -->

## Executive Summary

**Overall Rating:** [ ] Excellent [ ] Good [x] Fair [ ] Poor [ ] Critical

**Findings:** Critical: 0 | High: 0 | Medium: 3 | Low: 2

Logging is via SLF4J + Log4j 2 (`backend/pom.xml` excludes Logback in every starter and pulls `spring-boot-starter-log4j2`). Output is unstructured `PatternLayout` to stdout. The default `dev` profile enables Hibernate SQL trace logging — known to log bound parameter values.

---

## 1. Stack

| Item | Detail | Source |
|---|---|---|
| API | SLF4J | (transitive) |
| Implementation | Log4j 2 (Spring Boot starter) | `backend/pom.xml:77` |
| Configuration | `log4j2-spring.xml` (Console appender, PatternLayout) | `backend/src/main/resources/log4j2-spring.xml` |
| Default level | `INFO` root; `DEBUG` for `za.co.csnx.demo` | `log4j2-spring.xml:12, 14` |
| Dev profile override | `org.hibernate.SQL: DEBUG`, `org.hibernate.orm.jdbc.bind: TRACE` | `application-dev.yml:5-6` |
| Prod profile | `root: INFO`, no Hibernate noise | `application-prod.yml:1-3` |

---

## 2. Findings

### 2.1 Hibernate `bind: TRACE` logs PII in dev (default in compose)

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Medium | `org.hibernate.orm.jdbc.bind: TRACE` writes every bound prepared-statement parameter to stdout. For a login query, that includes the username (and indirectly the company-scoped principal). `docker-compose.yml:24` defaults to `SPRING_PROFILES_ACTIVE=dev`. | `application-dev.yml:6`; `docker-compose.yml:23-24` | Any "compose up" run that isn't explicitly overridden logs bind values, including authenticated identifiers, to stdout. |

### 2.2 SQL statement logging via JPA show-sql

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Medium | `show-sql: true` plus `format_sql: true` in dev. Combined with `bind: TRACE`, every query is duplicated and prettified into the log stream. | `application-dev.yml:8-12` | Log volume + minor redundancy; combined with §2.1, exacerbates the leak. |

### 2.3 Direct `System.out.println` in production code

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Medium | `System.out.println("fgfgfggf");` writes outside the logging pipeline. Cannot be redacted by a layout filter; cannot be downgraded by log level. | `backend/src/main/java/za/co/csnx/demo/DemoBackendApplication.java:16` | Operational hygiene; same finding as `back-end.md` §2.1. |

### 2.4 Successful and failed auth events are logged with usernames

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Low | Login success logs `company={} username={}` at INFO (`AuthService.java:42`); refresh logs `subject={}` at DEBUG (`AuthService.java:47`). Usernames are not secrets, but in some compliance frames (HIPAA, PCI) the username can be PII. The pair `company + username` is the data warehouse's primary key on `app_user`. | `backend/src/main/java/za/co/csnx/demo/service/AuthService.java:42, 47` | Low under typical conditions; rises if logs are shipped to a third-party SIEM without DPA. |
| Low | JWT parse failures log to DEBUG with `e.getMessage()` (`JwtService.java:73`) — fine, no token bytes in the log. The bearer-rejection log at `JwtAuthFilter.java:54` includes the method+URI but not the token. | `JwtService.java:72-74`; `JwtAuthFilter.java:54` | Acceptable. |

### 2.5 No structured logging (JSON layout)

| Severity | Status | Notes |
|---|---|---|
| Info | `log4j2-spring.xml` uses `PatternLayout` — plain text. Production deployments typically want JSON for ingest into Cloud Logging / Log Analytics / ELK. No central log-shipping config is present in the repo. |

### 2.6 No redaction filter for bearer tokens / cookies

| Severity | Status | Notes |
|---|---|---|
| Info | No `Log4j2 RewriteAppender` or pattern-based mask for `Authorization:` headers or `Set-Cookie:` lines. Spring Boot doesn't log request headers by default, so the practical risk is low — but a future request-logging filter would happily dump them. |

---

## 3. Recommendations

### Immediate
- Disable `bind: TRACE` even in dev (`application-dev.yml:6`) or move it to a separate `verbose` profile.
- Change `docker-compose.yml:24` to default to `prod` for compose runs, or to a `compose` profile without bind tracing.

### Short-term
- Replace `log4j2-spring.xml`'s `PatternLayout` with a JSON layout (`org.apache.logging.log4j:log4j-layout-template-json`) for production deployments.
- Add a `RewriteAppender` that masks `Authorization` / `Cookie` headers in any access-log style output.

### Long-term
- Centralise log shipping (Azure Log Analytics / GCP Cloud Logging — already on the deployment roadmap).
- Adopt a request-correlation ID (`MDC`) propagated through filters so per-request traces are linkable across services.

---

## Conclusion

Logging primitives are correct (SLF4J + Log4j 2, no Logback by design). The findings are configuration-level: dev defaults are too chatty, and the default compose profile pulls those settings into anything that resembles production use.
