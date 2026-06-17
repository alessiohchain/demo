---
genre: security
category: executive-summary
analysis-type: static
relevance:
  file-patterns: []
  keywords: []
  config-keys: []
  always-include: true
severity-scale: "Critical|High|Medium|Low|Info"
---

# Security Executive Summary

**Assessment Date:** 2026-05-26
**Prepared by:** code-audit (security-auditor agent)
**Project:** demo (Spring Boot + React reference module, CSNX-13935)

---

<!-- analysis: static -->

## Bottom line

The demo's **cryptographic and auth primitives are well-chosen** (bcrypt, HS256 JWT via jjwt 0.12.6, JPA-only data access, stateless session policy). The repository's **operational hygiene around secrets and defaults** is the failure mode: the JWT signing key has a published default value baked into `application.yml`, repeated in `docker-compose.yml`, and not overridden in `application-prod.yml`. Combined with default-seeded user credentials and verbose dev logging on the compose-default profile, the demo as it stands would not be safe to host customer data.

The good news: the remediations are mostly small, well-localised, and cascade across the same files. See `remediation-plan.md` for the ordered roadmap.

---

## Finding totals

| Severity | Count |
|---|---:|
| Critical | 2 |
| High | 7 |
| Medium | ~13 |
| Low | ~6 |
| Info | ~2 |
| **Total** | **~30** |

Counts approximate because some Highs and Mediums overlap across templates (e.g. the JWT-secret finding appears in `authentication`, `crypto-usage`, and `infrastructure` audits as the same root cause). Per `vulnerability-report.md`, the unique-finding count is **20**.

---

## Normalized metrics

Codebase size (excluded `node_modules`, `dist`, `target`):
- Backend Java: ~55 files, ~2 646 LOC.
- Frontend TS/TSX: ~90 files, ~1 938 LOC.
- Configuration / SQL / Docker / nginx: ~25 files, ~600 LOC.
- **Total LOC analysed: ~5 184**.

Normalised against ~5 200 LOC, with 20 unique findings:

| Metric | Value | Per 1 000 LOC |
|---|---:|---:|
| Critical findings | 2 | 0.385 |
| High findings | 7 | 1.346 |
| Medium findings | 6 | 1.154 |
| Low findings | 4 | 0.769 |
| Info findings | 1 | 0.192 |
| **Total** | **20** | **3.846** |

By the `audit-reviewer` rubric:
- Critical/1K LOC = 0.385 → exceeds Level 2 threshold (0.3) → **Level 1 (Critical, score 15)** on the security rubric.
- High/1K LOC = 1.346 → exceeds Level 2 threshold (2.0)? No — 1.346 is **within** Level 2's High band (≤2.0). The Critical-per-1000 metric is the binding constraint.
- Additional rule: "Authentication bypass or SQL injection Critical finding → cap at Level 2 (42)". VULN-001 is functionally an auth-bypass risk (known signing key ⇒ token forgery) — so the cap applies.

**Effective security score: 15** (Level 1, the auth-bypass cap is below the per-1K-LOC Level 1 threshold anyway).

Note: this score is computed for the audit-reviewer template that follows. The score will rise quickly once the two Critical findings are resolved — the underlying primitives are sound.

---

## Top 3 priorities

1. 🔴 **Eliminate the hardcoded JWT secret** — fix `application.yml:52`, `docker-compose.yml:28`, and add the override to `application-prod.yml`. Token forgery risk is the dominant exposure.
2. 🟡 **Remove the seeded default user from prod profiles** — `AppUserSeeder.java:28` ships `WCS/wcs/wcs123!` in every deployment.
3. 🟡 **Harden the refresh cookie** — `Secure` + `SameSite=Strict` + token rotation; also re-evaluate the CSRF-disabled posture.

---

## Top 3 strengths

1. **Password storage is correct.** bcrypt via Spring Security; per-hash salt; column wide enough. No legacy hashing.
2. **No SQL-injection vectors found.** All repository queries observed use parameterised Spring Data JPA / Specifications.
3. **Structured error envelope.** `GlobalExceptionHandler` returns RFC 7807 `ProblemDetail` with discriminated `kind` — consistent for the client, no stack traces in success paths.

---

## What manual testing should follow

Static analysis cannot answer these — flag for the next phase:
- Reproduce the JWT forgery against a default-secret deployment.
- Walk the service-layer queries (`LookupService`, `ProcessController`) for tenant-isolation predicates.
- Confirm `spring-boot-devtools` is excluded from the production fat-jar runtime classpath given the custom Dockerfile layering.
- Verify the `app_user` schema migration is present (the snapshot's `V1__init.sql` only creates `customer`).

---

## Closing

The demo is a fast-clone template. The findings in this report will replicate into every downstream module unless fixed at the source — make the Immediate items in `remediation-plan.md` the first commit on the next consumer. The Spring Security primitives are good; the surrounding `.yml` / `.env` story needs work.
