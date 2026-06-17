---
genre: security
category: access-control
analysis-type: static
relevance:
  file-patterns:
    - "**/rbac/**"
    - "**/roles/**"
    - "**/permissions/**"
    - "**/middleware/auth*"
    - "**/policies/**"
  keywords:
    - "rbac"
    - "role"
    - "permission"
    - "authorize"
    - "guard"
    - "policy"
    - "acl"
    - "casl"
  config-keys:
    - "casl"
    - "accesscontrol"
    - "casbin"
    - "@casl/ability"
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# Access Control Assessment

**Assessment Date:** 2026-05-26
**Auditor:** code-audit (security-auditor agent)
**Status:** Complete

---

<!-- analysis: static -->

## Executive Summary

**Overall Rating:** [ ] Excellent [ ] Good [ ] Fair [x] Poor [ ] Critical

**Findings:** Critical: 0 | High: 2 | Medium: 1 | Low: 0

The application implements **authentication** but not **authorization** beyond a binary "is logged in or not" check. Every authenticated user has identical access to every authenticated endpoint.

---

## 1. Authorization Model

### 1.1 What exists

- `JwtAuthFilter` parses the access token and populates `SecurityContextHolder` with a `UsernamePasswordAuthenticationToken(subject, null, List.of())` — **empty authorities** (`backend/src/main/java/za/co/csnx/demo/security/JwtAuthFilter.java:71`).
- `SecurityConfig.securityFilterChain` uses `anyRequest().authenticated()` — every URL under `/api/**` requires a valid bearer token but no further role check (`backend/src/main/java/za/co/csnx/demo/config/SecurityConfig.java:58`).
- No `@PreAuthorize`, `@PostAuthorize`, `@Secured`, `hasRole`, or `hasAuthority` usages in `backend/src/main/java/**` (grep'd).
- The user model has a `wcsUser` boolean (`domain/AppUser.java:56`) and a `locked` boolean (`domain/AppUser.java:53`) but no roles table.
- The frontend `UserInfo` interface declares `roles?: string[]` (`frontend/src/app/api/AuthProvider.tsx:29`) but the field is not populated by any backend response.

### 1.2 What is missing

- Role / permission catalogue.
- Per-endpoint authorization.
- Per-row authorization (tenant isolation — see Finding 2.2 below).

---

## 2. Findings

### 2.1 No per-endpoint RBAC

| Severity | Issue | Location | Impact |
|---|---|---|---|
| High | Authenticated users can call every `/api/**` endpoint regardless of role. No infrastructure exists to differentiate, say, a "wcsUser" admin from a regular user. | `JwtAuthFilter.java:71` (empty authorities) + `SecurityConfig.java:58` (anyRequest authenticated) | Any new admin endpoint added later is implicitly callable by every authenticated user. |

### 2.2 No tenant / company isolation in the data layer

| Severity | Issue | Location | Impact |
|---|---|---|---|
| High | The login flow stamps `"{companyCode}|{username}"` into the JWT subject (`security/AppUserDetailsService.java:50`, `service/AuthService.java:39-43`) and `AuditingConfig` extracts the *username* part for audit columns. However, the audit found no evidence that downstream queries filter by `companyCode` from the principal — the controllers read by this audit (`AuthController`, `LookupController`) do not call `AppUserDetailsService.companyOf(principal)` to bound their queries. | `backend/src/main/java/za/co/csnx/demo/common/AuditingConfig.java:25` reads `usernameOf(...)`; no caller of `companyOf(...)` was found in the read sample | If any `LookupService` / `ProcessService` query loads rows without filtering by the principal's company, a user from company A can read company B's data. **Manual verification required** in `service/LookupService.java` and `web/ProcessController.java` (not fully read in this sweep). |

### 2.3 `wcsUser` flag has no enforcement path

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Medium | `AppUser.wcsUser` is a boolean intended (per `AppUserSeeder.java:29` comment and the column's name) to mark internal staff users with elevated privileges. No code reads `getWcsUser()` to grant or deny anything. | `backend/src/main/java/za/co/csnx/demo/domain/AppUser.java:56` (defined); no readers found | A future privilege boundary based on `wcsUser` is unenforced. The column is dead capacity. |

---

## 3. Recommendations

### Immediate
- Document explicitly that the demo has *no* authorization model beyond authentication, so consumers cloning the template don't assume RBAC is in place.

### Short-term
- Add a `roles` (or `authorities`) JSON-array claim to the JWT.
- Populate `UsernamePasswordAuthenticationToken` with `SimpleGrantedAuthority`s from the claim in `JwtAuthFilter.authenticate(...)`.
- Enable `@EnableMethodSecurity` on a `@Configuration` class; annotate write endpoints with `@PreAuthorize("hasRole('ADMIN')")`.
- Audit every JPA repository call for company-scope filtering; add a Hibernate `@Filter` or a global `Specification` that injects the principal's `companyCode` predicate.

### Long-term
- Move to attribute-based access control (ABAC) if the domain grows beyond company-level segmentation.
- Add row-level security in Postgres as defence-in-depth.

---

## Conclusion

The demo authenticates users correctly and stamps the company-code into the principal but does not yet **act on** that scope anywhere downstream. The two High-severity findings represent the gap between "the data model knows about multi-tenancy" and "the runtime enforces it". Manual verification of the service-layer queries is required before any production use.
