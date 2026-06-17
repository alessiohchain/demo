---
genre: infrastructure
category: authentication
analysis-type: static
relevance:
  file-patterns:
    - "**/auth/**"
    - "**/login/**"
    - "**/middleware/auth*"
  keywords:
    - "jwt"
    - "oauth"
    - "session"
    - "passport"
    - "bcrypt"
  config-keys:
    - "passport"
    - "jsonwebtoken"
    - "@auth0"
    - "bcrypt"
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# Authentication Infrastructure Maturity

**Assessment Date:** 2026-05-26
**Status:** Complete

---

<!-- analysis: static -->

## Maturity score — **3 / 5 (Functional)**

Modern primitives (Spring Security 6, jjwt, bcrypt, stateless JWT + refresh cookie) are in place and used idiomatically. The auth *flow* is correct. Maturity is held back by **the secret-management posture** and the absence of operational controls (rate limit, lockout, rotation, blacklist).

| Item | State | Evidence |
|---|---|---|
| Stateless session policy | Yes | `backend/src/main/java/za/co/csnx/demo/config/SecurityConfig.java:43` |
| Password hash | bcrypt via `BCryptPasswordEncoder` | `backend/src/main/java/za/co/csnx/demo/config/SecurityConfig.java:25` |
| Bearer JWT | HS256 via jjwt 0.12.6 | `backend/src/main/java/za/co/csnx/demo/security/JwtService.java:28-56` |
| Refresh cookie | HttpOnly, scoped to `/api/auth` | `backend/src/main/java/za/co/csnx/demo/web/AuthController.java:82-87` |
| Token-type discrimination | `typ` claim (`access`/`refresh`) | `backend/src/main/java/za/co/csnx/demo/security/JwtService.java:19-21, 67-69` |
| Multi-tenant principal | `{companyCode}|{username}` | `backend/src/main/java/za/co/csnx/demo/security/AppUserDetailsService.java:23, 50` |
| Auditing | `JpaAuditing` reads username from principal | `backend/src/main/java/za/co/csnx/demo/common/AuditingConfig.java:25` |
| OpenAPI Bearer scheme | Declared | `backend/src/main/java/za/co/csnx/demo/config/OpenApiConfig.java:25-29` |

---

## Operational gaps (drag the maturity score down)

| Gap | Impact on maturity score |
|---|---|
| Hardcoded JWT secret default (see security audits) | -1 |
| Default seeded user `WCS/wcs/wcs123!` | -1 |
| Refresh cookie missing `Secure` + `SameSite` | -0.5 |
| No rate limit, no lockout, no MFA | -1 |
| Refresh-token not rotated; access JWT not revocable on logout | -0.5 |

(Cumulative drag from a baseline of 5 → land at 3 after rounding.)

---

## Strengths

- Type-discriminated tokens prevent refresh-as-access confusion (catches a class of jjwt misuse).
- `AuditingConfig` cleanly separates principal composition (`companyCode|username` in JWT subject) from audit-column population (just `username`).
- `OpenAPI` doc declares the Bearer scheme so Swagger UI sends `Authorization` correctly.
- `JwtAuthFilter` has an explicit public-paths whitelist (`/api/auth/**`, `/v3/api-docs/**`, `/actuator/health`, `/actuator/info`) — easier to audit than relying on `permitAll` antMatchers.

---

## Findings

(Cross-references the more detailed security `authentication.md` — only re-summarised here to inform the maturity score.)

| Severity | Issue | Location | Recommendation |
|---|---|---|---|
| High | Secret-management posture: dev default in `application.yml`, repeated in `docker-compose.yml`, not overridden in `application-prod.yml`. | See security audit. | Phase-I remediation. |
| High | No rate limit / lockout. | (no filter in `SecurityConfig`) | `bucket4j-spring-boot-starter`. |
| Medium | Refresh-cookie attributes incomplete. | `AuthController.java:82-87` | Use `ResponseCookie`. |
| Medium | Empty authorities on principal — no RBAC. | `JwtAuthFilter.java:71` | Add `roles` claim. |
| Medium | No refresh-token rotation; no access-token revocation. | `AuthService.java:46-49`; `AuthController.java:72-80` | Phase-II. |
| Low | No MFA. | (absent by design) | Phase-III. |

---

## Maturity verdict — **3**

Modern auth design, but the secret-management story and missing operational controls drag this firmly to "Functional" until remediation phase 1 lands.
