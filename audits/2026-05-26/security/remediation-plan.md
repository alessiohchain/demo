---
genre: security
category: remediation-plan
analysis-type: static
relevance:
  file-patterns: []
  keywords: []
  config-keys: []
  always-include: true
severity-scale: "Critical|High|Medium|Low|Info"
---

# Security Remediation Plan

**Assessment Date:** 2026-05-26
**Prepared by:** code-audit (security-auditor agent)

---

<!-- analysis: static -->

This plan groups the 20 findings in `vulnerability-report.md` into an ordered remediation roadmap. The cadence reflects the demo's posture: a reference module that other modules clone — so quick fixes ripple to every downstream consumer.

---

## 🔴 Immediate (within 7 days) — Critical & High

| # | Action | Vulns addressed | Effort | Owner |
|---|---|---|---|---|
| I-1 | Remove the hardcoded default value at `application.yml:52`. Let the app fail-fast if `APP_SECURITY_JWT_SECRET` is missing. | VULN-001 | S | Backend |
| I-2 | Rewrite `docker-compose.yml:28` to require an external secret (`${APP_SECURITY_JWT_SECRET:?set in .env}`) and add a `.env.example` documenting the variable. | VULN-002 | S | Backend / Ops |
| I-3 | Add `app.security.jwt.secret: ${APP_SECURITY_JWT_SECRET}` (no default) to `application-prod.yml`. | VULN-003 | S | Backend |
| I-4 | Set `Secure` + `SameSite=Strict` on the refresh cookie. Migrate `AuthController.java:82-87` to `ResponseCookie`. | VULN-005 | S | Backend |
| I-5 | Replace `POSTGRES_PASSWORD: demo` / `DB_PASSWORD: demo` literals in `docker-compose.yml` with `${POSTGRES_PASSWORD:?}`. Remove defaults at `application.yml:8-9`. | VULN-009 | S | Backend / Ops |
| I-6 | Gate `AppUserSeeder` behind `@Profile("dev")`. | VULN-004 | S | Backend |
| I-7 | Delete `System.out.println("fgfgfggf");` at `DemoBackendApplication.java:16`. | VULN-014 | XS | Backend |
| I-8 | Switch `docker-compose.yml:24` to default `SPRING_PROFILES_ACTIVE=prod` (or to a new `compose` profile that doesn't enable `bind: TRACE`). | VULN-011 | S | Ops |
| I-9 | Bind Postgres to `127.0.0.1:5432` in compose (or drop the host mapping entirely). | VULN-009 | XS | Ops |

**Sizing key:** XS = minutes, S = hours, M = day, L = multi-day, XL = week+.

---

## 🟡 Short-term (1–4 weeks) — High & Medium

| # | Action | Vulns addressed | Effort | Owner |
|---|---|---|---|---|
| S-1 | Add `bucket4j-spring-boot-starter` rate-limiter on `/api/auth/login`, `/api/auth/refresh`, and `/api/lookup/init`. Increment an `app_user.failed_login_count` and auto-lock after 10 failures. | VULN-007 | M | Backend |
| S-2 | Add a `roles` claim to the JWT. Populate `SimpleGrantedAuthority`s in `JwtAuthFilter.authenticate(...)`. Enable `@EnableMethodSecurity`. Annotate write endpoints with `@PreAuthorize`. | VULN-008 | M | Backend |
| S-3 | Audit every JPA query for company-scope filtering. Introduce a `CompanyScopedSpecification` injected from the security context. | VULN-010 | L | Backend |
| S-4 | Replace `setAllowedHeaders(List.of("*"))` with an explicit whitelist (`Authorization`, `Content-Type`, `X-Requested-With`). | VULN-012 | XS | Backend |
| S-5 | Implement refresh-token rotation. Persist a `refresh_token_jti` per user; reject any refresh whose JTI differs. | VULN-015 | M | Backend |
| S-6 | Add a JWT blacklist (Caffeine cache or Redis) consulted by `JwtAuthFilter`. Logout pushes the access-token JTI onto the blacklist. | VULN-016 | M | Backend |
| S-7 | Disable Swagger UI in prod via `@Profile("!prod")` on `OpenApiConfig`. | VULN-017 | XS | Backend |
| S-8 | Add baseline security headers (CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy) in both `frontend/nginx.conf.template` and Spring Security's `HeadersConfigurer`. | VULN-018 | S | Backend / FE |
| S-9 | Set `server.error.include-message: never` in `application-prod.yml`. Sanitise `IllegalArgumentException`/`EntityNotFoundException` handlers to use generic messages in prod. | VULN-013 | S | Backend |
| S-10 | Enable Dependabot for `pom.xml` and `package.json`. Add `dependency-check-maven` and `npm audit --omit=dev` to a new CI workflow. | VULN-020 | M | Ops |
| S-11 | Reconcile `README.md:96` (`/api/auth/register`) and `CLAUDE.md:41` (React 18 / Tailwind 3) with the actual code state. Wire generated-OpenAPI client to the frontend so future drift is compile-fail. | VULN-019 | M | FE / Docs |
| S-12 | Re-enable CSRF for cookie-bearing endpoints, or eliminate the cookie path entirely (move refresh token to a header + storage backend). | VULN-006 | M | Backend |

---

## 🟠 Medium-term (1–3 months) — Hardening

| # | Action | Vulns addressed | Effort |
|---|---|---|---|
| M-1 | Container hardening: `read_only: true`, `security_opt: ["no-new-privileges:true"]`, `cap_drop: [ALL]` per service. Run nginx as non-root in the frontend image. | (infrastructure) | M |
| M-2 | Tighten DTO validation: `@Pattern` on `companyCode`, `@Size` on email/password fields, custom validator for password policy. | (auth + back-end) | S |
| M-3 | Replace the `|` delimiter in `principalOf()` with a non-printable separator. | (back-end) | XS |
| M-4 | Switch `log4j2-spring.xml` to a JSON layout; mask `Authorization` / `Cookie` headers via `RewriteAppender`. | (secure-logging) | M |
| M-5 | Add `dompurify` (and a regression test asserting `<script>` in server payloads renders inert) before the metadata-driven UI lands. | (ui-security) | S |
| M-6 | Document explicitly that the demo has no RBAC, so downstream module clones don't assume one. | (access-control) | XS |

---

## 🟢 Long-term (3–6 months) — Strategic

| # | Action | Notes |
|---|---|---|
| L-1 | Plan HS256 → RS256 migration with `kid`-based key rotation. | Allows multiple verifier instances without sharing the signing key. Wire to Key Vault / Secret Manager. |
| L-2 | Wire managed identity (Azure) / Workload Identity (GCP) so the backend never holds a Postgres password in env vars. | Pairs with `.azure/deployment-plan.md` / `.gcp/deployment-plan.md`. |
| L-3 | Add MFA (TOTP first) and a "forgot password" flow with single-use tokens. | Future production hardening. |
| L-4 | Adopt SBOM generation (`cyclonedx-maven-plugin`, `@cyclonedx/cyclonedx-npm`) and signed releases. | Pairs with VULN-020. |
| L-5 | Postgres row-level security on tenant-scoped tables as defence-in-depth for VULN-010. | |

---

## Verification

After each phase, re-run this audit (`/code-audit` from the repo root, or this prompts-only bundle from a sibling directory). Specifically:

- After Phase 1: verify the per-template counts for Critical drop to 0.
- After Phase 2: verify per-1000-LOC High count is ≤ 0.3 (Level 4 on the scoring rubric).
- After Phase 3: target overall health score ≥ 75.
