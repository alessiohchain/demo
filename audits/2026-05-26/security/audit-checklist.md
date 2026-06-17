---
genre: security
category: audit-checklist
analysis-type: static
relevance:
  file-patterns: []
  keywords: []
  config-keys: []
  always-include: true
severity-scale: "Critical|High|Medium|Low|Info"
---

# Security Audit Checklist

**Assessment Date:** 2026-05-26
**Auditor:** code-audit (security-auditor agent)
**Status:** Complete

---

<!-- analysis: static -->

## OWASP Top 10 (2021) Coverage

| # | Risk | Status | Evidence / Source |
|---|---|---|---|
| A01 | Broken Access Control | **Fail** | No RBAC; binary auth-only. JWT filter assigns empty authorities. See `access-control.md`. |
| A02 | Cryptographic Failures | **Fail** | JWT signing secret hardcoded in repo. See `crypto-usage.md` & `authentication.md`. |
| A03 | Injection | **Pass (partial)** | No SQL injection vectors found (Spring Data JPA throughout). No template-injection patterns. ORM safe; raw SQL absent. |
| A04 | Insecure Design | **Partial** | Brute-force / lockout missing (`authentication.md` §5). Secret-management mode is repo-committed. |
| A05 | Security Misconfiguration | **Fail** | Dev defaults in `application.yml`, `docker-compose.yml`. Swagger public in prod profile. Verbose error responses. See `back-end.md` & `infrastructure.md`. |
| A06 | Vulnerable / Outdated Components | **Pass (acceptable today)** | Dependencies are current. Process gap: no scanner / Dependabot. See `third-party-dependencies.md`. |
| A07 | Identification & Authentication Failures | **Fail** | Seeded user with known credentials, no rate-limit, no MFA, refresh-token rotation missing. See `authentication.md`. |
| A08 | Software & Data Integrity Failures | **Partial** | No SBOM, no SRI, no signed releases. Backend deps come from Maven Central transitively (acceptable). |
| A09 | Security Logging & Monitoring Failures | **Fail** | Verbose dev defaults, no central log shipping, no security-relevant audit events (failed logins not counted; lockouts not emitted). See `secure-logging.md`. |
| A10 | Server-Side Request Forgery (SSRF) | **N/A** | Backend does not make outbound HTTP calls based on user input in the code paths read. |

---

## ASVS Level 1 Quick Gate

| Section | Pass | Notes |
|---|---|---|
| V2 Authentication | Partial | Password storage strong; brute-force protection absent; default user seeded. |
| V3 Session Management | Partial | JWT lifecycle defined; cookie flags incomplete. |
| V4 Access Control | Fail | No per-endpoint authorization. |
| V5 Validation, Sanitization, Encoding | Partial | Bean Validation present but minimal on critical DTOs. |
| V7 Error Handling & Logging | Partial | RFC 7807 envelope is good; verbose error messages and bind-trace logging are concerns. |
| V8 Data Protection | Partial | No TLS enforcement in JDBC URL; secret management weak. |
| V9 Communication | Partial | TLS terminated upstream; no in-app HSTS / CSP. |
| V14 Configuration | Fail | Multiple hardcoded defaults; dev profile mixed into compose default. |

---

## Quick Findings Summary

| Severity | Count | Headline |
|---|---|---|
| Critical | 2 | JWT secret hardcoded (`application.yml`, `docker-compose.yml`). |
| High | 5 | Seeded user, refresh cookie missing `Secure`/`SameSite`, CSRF disabled with cookie auth, no rate-limit, no per-endpoint RBAC. |
| Medium | ~15 | CORS too permissive; verbose dev logging; weak DTO validation; debug `println`; documentation drift; etc. |
| Low | ~8 | Swagger public in prod; no CSP; no SBOM; etc. |
| Info | a few | Documentation freshness (CLAUDE.md vs package.json); structured logging gap. |

(Counts approximate — see `vulnerability-report.md` and individual templates for exact lists.)

---

## Manual Tests Still Required

The following cannot be answered by static analysis and need a manual or dynamic pass:

1. Verify the `app_user` schema is created by a migration not present in this snapshot (the `V1__init.sql` only creates `customer`; `AppUser` is mapped to `demoschema.app_user`).
2. Confirm `spring-boot-devtools` is not actually shipping in the prod fat jar (the custom Dockerfile copies `BOOT-INF/lib` directly).
3. Confirm whether `/api/auth/change-password` and `/api/auth/register` are wired to handlers (the frontend calls the former; README documents the latter — neither handler is in the read sample of `AuthController.java`).
4. Replay a captured refresh-cookie cross-origin against `/api/auth/refresh` to confirm the CSRF + SameSite gap is exploitable.
5. Walk every `service/` query for company-code-scoped predicates (tenant isolation).

---

## Recommendations Roll-up

See `remediation-plan.md` and `executive-summary.md` for prioritised actions.
