---
genre: infrastructure
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
    - "sentry"
  config-keys:
    - "winston"
    - "pino"
    - "bunyan"
    - "morgan"
    - "@sentry/node"
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# Logging Infrastructure Maturity

**Assessment Date:** 2026-05-26
**Status:** Complete

---

<!-- analysis: static -->

## Maturity score — **2 / 5 (Outdated)**

Logging primitives are correct (SLF4J + Log4j 2, intentional exclusion of Logback across every Boot starter). The configuration is **stdout-only with `PatternLayout`** — no JSON layout, no MDC, no log shipping, no redaction, and the dev profile (default in compose) enables `bind: TRACE` which logs PII.

| Item | State | Source |
|---|---|---|
| API | SLF4J | (transitive) |
| Implementation | Log4j 2 | `backend/pom.xml:77` |
| Logback excluded | Yes — across every starter | `backend/pom.xml:28-33, 39-43, 49-53, 59-63, 69-73` |
| Config file | `log4j2-spring.xml` (Console + PatternLayout) | `backend/src/main/resources/log4j2-spring.xml` |
| Default level | `INFO` root, `DEBUG` for `za.co.csnx.demo` | `log4j2-spring.xml:12, 14` |
| Dev profile overrides | `org.hibernate.SQL: DEBUG`, `org.hibernate.orm.jdbc.bind: TRACE` | `application-dev.yml:5-6` |
| Prod profile | `root: INFO` | `application-prod.yml:1-3` |
| JSON layout | No (PatternLayout) | `log4j2-spring.xml:4, 9` |
| MDC / correlation IDs | None | (no filter adds correlation) |
| Log redaction | None | (no `RewriteAppender`) |
| Frontend logging | Only `console.error` via sonner toasts; no client-side log shipping | `frontend/src/app/api/client.ts:167, 177, 181-184` |

---

## Findings

| Severity | Issue | Location | Recommendation |
|---|---|---|---|
| High | `org.hibernate.orm.jdbc.bind: TRACE` (dev profile, which is the compose default) logs every prepared-statement bind value. | `application-dev.yml:6`; `docker-compose.yml:23-24` | Move to a `verbose` profile; switch compose default to `prod`. |
| Medium | PatternLayout, not JSON — production log aggregators want JSON. | `log4j2-spring.xml:4-9` | Add `log4j-layout-template-json` and a JSON layout. |
| Medium | No MDC / request-correlation IDs. Tracing across services / instances would require it. | (no filter adds `MDC.put`) | Add a `ServletRequestListener` / `Filter` that populates `MDC` with `X-Request-Id` or generates one. |
| Medium | `System.out.println("fgfgfggf");` bypasses the logging pipeline. | `backend/src/main/java/za/co/csnx/demo/DemoBackendApplication.java:16` | Delete (also flagged in security). |
| Low | No central log shipping configured. | (no `cloud-logging` / `application-insights` dependencies) | Wire to Azure Log Analytics / GCP Cloud Logging in the deployment-plan phase. |
| Low | No bearer-token / cookie redaction filter. The backend doesn't log request headers today, but a future access-log filter would happily dump them. | (no `RewriteAppender` in `log4j2-spring.xml`) | Pre-emptive add a redactor for `Authorization` and `Cookie`. |

---

## Strengths

- Single logging implementation (Log4j 2) — no SLF4J binding ambiguity.
- Loggers per package (`za.co.csnx.demo: DEBUG`, `org.springframework: INFO`) — sensible defaults.
- All code uses `LoggerFactory.getLogger(MyClass.class)` (per `CLAUDE.md:88-89`). No `System.out` in service / web / security layers — only the one in `DemoBackendApplication.java`.
- Login / refresh / token-parse events are logged at sensible levels (`INFO` success, `DEBUG` failure detail).

---

## Maturity verdict — **2**

Implementation choice is correct but the configuration is dev-grade. The Phase-I remediations (compose default + bind level) plus the JSON layout would lift this to 4.
