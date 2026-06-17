---
genre: infrastructure
category: access-control
analysis-type: static
relevance:
  file-patterns:
    - "**/rbac/**"
    - "**/roles/**"
    - "**/permissions/**"
  keywords:
    - "rbac"
    - "role"
    - "permission"
    - "authorize"
    - "guard"
    - "policy"
  config-keys:
    - "casl"
    - "accesscontrol"
    - "casbin"
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# Access-Control Infrastructure Maturity

**Assessment Date:** 2026-05-26
**Status:** Complete

---

<!-- analysis: static -->

## Maturity score — **1 / 5 (Legacy / Critical gaps)**

There is **no authorization model** beyond authentication. The principal carries an empty authorities list. There are no roles, no permissions, no policy abstractions, no per-endpoint `@PreAuthorize`. The data model has a `wcsUser` boolean column that is never consulted at runtime.

| Item | Present? | Source |
|---|---|---|
| Per-endpoint RBAC | No | `backend/src/main/java/za/co/csnx/demo/config/SecurityConfig.java:58` (`anyRequest().authenticated()`) |
| Method-level `@PreAuthorize` / `@Secured` | No | grep'd `backend/src/main/java/**` — no matches |
| Roles claim in JWT | No | `JwtService.java:49-56` (only `iss`, `sub`, `typ`, `iat`, `exp`) |
| Authorities populated in `SecurityContext` | No | `JwtAuthFilter.java:71` — `List.of()` |
| Role table in DB | No | `V1__init.sql` (no roles / permissions tables) |
| Frontend `UserInfo.roles` populated | No | `AuthProvider.tsx:29` declares `roles?: string[]` but backend response does not set it |
| Tenant scoping in queries | Partial | `AppUserRepository.findByCompanyCodeAndUsername` does scope by company, but the audit found no evidence that other repository methods inject the principal's company-code automatically |

---

## What does exist

- The principal stamps `{companyCode}|{username}` into the JWT subject so audit columns (`created_by`, `updated_by`) can record the username scoped to the company.
- The `app_user` table has a `locked` column honoured by `AppUserDetailsService.java:40` — read but never set by code.
- The `app_user` table has a `wcs_user` column intended to mark internal staff with elevated rights — never read.

---

## Findings

| Severity | Issue | Location | Recommendation |
|---|---|---|---|
| High | No per-endpoint RBAC; every authenticated user can call every authenticated endpoint. | `backend/src/main/java/za/co/csnx/demo/security/JwtAuthFilter.java:71`; `backend/src/main/java/za/co/csnx/demo/config/SecurityConfig.java:58` | Add `roles` claim → populate `SimpleGrantedAuthority` → enable `@EnableMethodSecurity` → annotate endpoints. |
| High | Tenant isolation cannot be verified — `companyOf(principal)` is defined but no caller was found in the read sample. Cross-company reads may be possible in the `LookupService` / `ProcessController` paths not deeply audited. | `backend/src/main/java/za/co/csnx/demo/security/AppUserDetailsService.java:62-66` | Manual audit of every JPA query that doesn't already filter by `companyCode`. Consider a Hibernate `@Filter` parametrised by the principal. |
| Medium | The `wcsUser` boolean is read-only column capacity — no enforcement path. | `backend/src/main/java/za/co/csnx/demo/domain/AppUser.java:56` | Either wire it into a "wcsUser-only" role or delete it from the entity. |
| Medium | The `locked` boolean is honoured at login but never written. | `backend/src/main/java/za/co/csnx/demo/security/AppUserDetailsService.java:40` (read); no writers | Pair with the rate-limit / lockout work in `authentication.md`. |

---

## Maturity verdict — **1**

This is the lowest-scoring dimension of the audit. The minimum-dimension penalty in the audit-reviewer rubric (5 × max(0, 3 − 1) = 10 points) applies here.

Add even a basic `ROLE_USER` / `ROLE_ADMIN` distinction and this lifts to 2; full per-endpoint authorities lift it to 3.
