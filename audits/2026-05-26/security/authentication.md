---
genre: security
category: authentication
analysis-type: static
relevance:
  file-patterns:
    - "**/auth/**"
    - "**/login/**"
    - "**/middleware/auth*"
    - "**/passport*"
  keywords:
    - "jwt"
    - "oauth"
    - "session"
    - "passport"
    - "bcrypt"
    - "argon2"
    - "saml"
    - "mfa"
    - "totp"
  config-keys:
    - "passport"
    - "jsonwebtoken"
    - "@auth0"
    - "bcrypt"
    - "argon2"
    - "express-session"
    - "next-auth"
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# Authentication Security Assessment

**Assessment Date:** 2026-05-26
**Auditor:** code-audit (security-auditor agent)
**Application:** demo-backend 0.0.1-SNAPSHOT + demo-frontend 0.0.0
**Status:** Complete

---

<!-- analysis: static -->

## Executive Summary

**Overall Authentication Security Rating:** [ ] Excellent [ ] Good [ ] Fair [x] Poor [ ] Critical

**Key Findings:**
- Total Vulnerabilities: 7
- Critical: 2 | High: 3 | Medium: 2 | Low: 0

**Most Critical Issue:** The JWT signing secret has a hardcoded base64 default in `application.yml` AND is shipped literally in `docker-compose.yml`. Any deployment that forgets to set `APP_SECURITY_JWT_SECRET` runs with the documented "dev secret — replace in production" value, allowing any attacker who reads the repo to forge access tokens.

---

## Scope

### Components Assessed
- [x] Password policies and storage
- [ ] Multi-factor authentication (MFA) — not implemented (out of scope by design)
- [x] Session management (JWT bearer + refresh cookie)
- [ ] OAuth 2.0 / OpenID Connect implementation — not used
- [ ] SAML authentication — not used
- [ ] Brute force protection — not implemented
- [x] Password reset flows (change-password endpoint exists)
- [ ] Account lockout mechanisms — `locked` column exists, no automatic lockout
- [ ] Single Sign-On (SSO) integration — not used

### Out of Scope
- MFA, OAuth, SAML, SSO — the demo intentionally uses local JWT auth only. Marked N/A throughout.

---

## 1. Password Security

### 1.1 Password Policies

**Finding:** [ ] Pass [x] Fail [ ] N/A

**Assessment:**
- [ ] Minimum password length enforced (12+ characters) — not enforced server-side
- [ ] Password complexity requirements are reasonable — defined by `passwordSettings` payload only (client-trusted)
- [ ] Password history prevents reuse — not implemented
- [ ] Maximum password age is enforced — not implemented
- [ ] Common/breached passwords are blocked — not implemented
- [ ] No maximum password length restriction (<64 chars)

**Issues Found:**

| Severity | Issue | Location | Committed By | Approved By | Impact |
|----------|-------|----------|--------------|-------------|--------|
| High | Seeded user `WCS/wcs` ships with hardcoded password `wcs123!` on every startup | `backend/src/main/java/za/co/csnx/demo/service/AppUserSeeder.java:28` | Unknown (no git history available) | Unknown | Default credential exists in every deployment; attacker can authenticate as `WCS/wcs` immediately |
| Medium | No server-side password policy enforcement; rules are sent to client via `PasswordSettings` and only validated there | `frontend/src/app/auth/passwordRules.ts` (rules definition); `backend/src/main/java/za/co/csnx/demo/web/dto/LoginRequest.java:17` (only `@NotBlank`) | Unknown | Unknown | Server accepts any non-blank password the client submits |

**Current Policy:**
```
Minimum Length: not enforced on server (frontend-only `passwordSettings.minLength`)
Complexity: not enforced on server
History: not implemented
Max Age: not implemented
```

**Recommendations:**
- Remove the `AppUserSeeder` seed for production profiles, or gate it behind `@Profile("dev")`.
- Add server-side bean-validation constraints (`@Size(min = 12)` + custom validator for mixed case / digits) to the change-password and any future register DTO.
- Add a deny-list / HIBP integration before persisting a new hash.

### 1.2 Password Storage

**Finding:** [x] Pass [ ] Fail [ ] N/A

**Assessment:**
- [x] Passwords are hashed with bcrypt (`BCryptPasswordEncoder`)
- [x] Salt is unique per password (bcrypt embeds per-hash salt)
- [x] Work factor is appropriate (Spring Security default = 10 cost; acceptable)
- [x] Passwords are never logged or stored in plaintext
- [x] Password hashes are not exposed in API responses (DTO mapping excludes `passwordHash`)
- [x] Schema column wide enough for bcrypt (`password_hash VARCHAR(255)`)

**Hashing Configuration:**
```
Algorithm: bcrypt
Cost Factor: 10 (Spring Security default for new BCryptPasswordEncoder())
Salt Method: per-password (bcrypt-native)
Source:    backend/src/main/java/za/co/csnx/demo/config/SecurityConfig.java:25
Schema:    backend/src/main/resources/db/migration/V1__init.sql:8
Entity:    backend/src/main/java/za/co/csnx/demo/domain/AppUser.java:38
```

**Recommendations:**
- Consider raising the cost factor to 12 (`new BCryptPasswordEncoder(12)`); demo is acceptable at 10 but production should be at least 12 on modern hardware.

---

## 2. Multi-Factor Authentication (MFA)

**Finding:** [ ] Pass [ ] Fail [x] N/A

MFA is not implemented. The application uses single-factor username+password authentication only. This is an explicit design choice for the demo but should be re-assessed before any production hardening.

**Recommendations:**
- Add TOTP-based MFA as a follow-up for production-grade deployments.

---

## 3. Session Management

### 3.1 Session Creation & Storage

**Finding:** [ ] Pass [x] Fail [ ] N/A

The application is **stateless** — sessions are not stored server-side. Authentication state lives in two tokens:
- **Access token** (JWT, 1 h TTL) — held in browser memory by the SPA (`frontend/src/app/api/client.ts:9`).
- **Refresh token** (JWT, 7 d TTL) — held in an HTTP cookie named `refreshToken`, set in `AuthController.java:82-87`.

**Assessment:**
- [x] Session IDs (JWTs) are signed with HS256 against a server-side key
- [x] Access token TTL is short (1 h, `application.yml:49`)
- [x] Refresh cookie is `HttpOnly` (`AuthController.java:75, 84`)
- [ ] **Refresh cookie is NOT `Secure`** — no `cookie.setSecure(true)` call
- [ ] **Refresh cookie has NO `SameSite` attribute** — Spring's default for `jakarta.servlet.http.Cookie` does not set it
- [ ] Session-fixation prevention is N/A for stateless JWT; no rotation of refresh token on use

**Issues Found:**

| Severity | Issue | Location | Committed By | Approved By | Impact |
|----------|-------|----------|--------------|-------------|--------|
| High | Refresh cookie set without `Secure` flag — transmitted over plaintext HTTP if TLS is misconfigured | `backend/src/main/java/za/co/csnx/demo/web/AuthController.java:82-87` | Unknown | Unknown | Refresh token can be intercepted on the wire; attacker gains 7-day forge capability |
| High | Refresh cookie set without `SameSite` attribute — browsers default to `Lax` in modern Chromium but not all clients; relying on default is brittle | `backend/src/main/java/za/co/csnx/demo/web/AuthController.java:82-87` | Unknown | Unknown | CSRF risk on `/api/auth/refresh` — a malicious cross-site request that triggers a POST would silently rotate tokens, since CSRF protection is disabled (see SecurityConfig:42) and the cookie is auto-sent |
| Medium | Refresh tokens are NOT rotated on use — `AuthService.refresh()` simply re-issues a fresh refresh JWT with no invalidation of the prior one | `backend/src/main/java/za/co/csnx/demo/service/AuthService.java:46-49` | Unknown | Unknown | A stolen refresh token remains valid until natural expiry (7 d); no detection if both legitimate user and attacker hold the same token |

**Session Configuration:**
```
Storage: stateless (no server-side session — SessionCreationPolicy.STATELESS at SecurityConfig.java:43)
Cookie:  refreshToken, HttpOnly=true, Path=/api/auth, Secure=false, SameSite=<unset>
Access TTL:  PT1H  (3600 s)
Refresh TTL: P7D   (604 800 s)
```

**Recommendations:**
- Set `cookie.setSecure(true)` and `cookie.setAttribute("SameSite", "Strict")` (or use Spring's `ResponseCookie.from(...).secure(true).sameSite("Strict").build()` for proper attribute support).
- Implement refresh-token rotation with a server-side blacklist or a per-user refresh-token ID stored in the database; revoke the prior token on every refresh.

### 3.2 Session Lifecycle

**Finding:** [ ] Pass [x] Fail [ ] N/A

**Assessment:**
- [x] Absolute timeout enforced via JWT `exp` (1 h access, 7 d refresh)
- [ ] Idle timeout is NOT enforced — JWT cannot be revoked early
- [ ] Logout does NOT invalidate access token server-side — `AuthController.logout()` only clears the refresh cookie; an attacker with a stolen access token can keep using it for up to 1 h
- [ ] Concurrent session limits are not enforceable on a stateless JWT scheme without external storage
- [x] `STATELESS` session policy prevents session-fixation

**Issues Found:**

| Severity | Issue | Location | Committed By | Approved By | Impact |
|----------|-------|----------|--------------|-------------|--------|
| Medium | Logout only clears refresh cookie; the active access JWT remains valid until natural expiry (up to 1 h) | `backend/src/main/java/za/co/csnx/demo/web/AuthController.java:72-80` | Unknown | Unknown | "Sign out" gives users a false sense of security; a stolen bearer token is still usable post-logout |

**Recommendations:**
- Add a server-side JTI blacklist (Redis) checked by `JwtAuthFilter` — push the access-token JTI on logout, evict at `exp`. Or shorten access TTL further (5–15 min) and accept the trade-off.

---

## 4. OAuth 2.0 / OpenID Connect & JWT

### 4.1 OAuth Implementation

**Finding:** [ ] Pass [ ] Fail [x] N/A

No OAuth/OIDC integration; the app is the IdP for itself.

### 4.2 JWT Token Security

**Finding:** [ ] Pass [x] Fail [ ] N/A

**Assessment:**
- [x] JWT signatures are validated on every authenticated request (`JwtAuthFilter.java:52`)
- [x] Token expiration (`exp`) is checked (jjwt verifies by default during `parseSignedClaims`)
- [x] Issuer is verified (`JwtService.java:63`)
- [x] Token type discriminator (`typ` claim) prevents refresh-as-access confusion (`JwtService.java:67-69`)
- [ ] **Signing algorithm is HS256 (symmetric)** — same key signs and verifies; any compromise of the secret = full forgery
- [ ] Token revocation is NOT supported
- [x] Sensitive data not stored in JWT payload (only `sub` + `typ` + standard claims)

**Issues Found:**

| Severity | Issue | Location | Committed By | Approved By | Impact |
|----------|-------|----------|--------------|-------------|--------|
| **Critical** | JWT signing secret defaults to a hardcoded base64 value `dGhpcy1pcy1hLWRldi1zZWNyZXQtcmVwbGFjZS1pbi1wcm9kdWN0aW9uLW9rPw==` (decodes to "this-is-a-dev-secret-replace-in-production-ok?") if `APP_SECURITY_JWT_SECRET` is unset | `backend/src/main/resources/application.yml:52` | Unknown | Unknown | Any deployment that forgets the env var runs with a known secret; attackers can forge arbitrary access tokens for any user |
| **Critical** | The same dev secret is shipped verbatim in `docker-compose.yml`, so the local stack always runs with a known key, and the file is the *example* operators copy into other environments | `docker-compose.yml:28` | Unknown | Unknown | Compose-based deployments inherit the dev secret unless someone manually rewrites the file before deploy |
| High | `application-prod.yml` does **not** override `app.security.jwt.secret` — a `prod` profile launched without the env var silently falls through to the dev default in `application.yml` | `backend/src/main/resources/application-prod.yml` (file lacks `app.security.jwt.secret`) | Unknown | Unknown | Production deployments with incomplete env wiring expose the same forge risk |

**JWT Configuration:**
```
Library:           io.jsonwebtoken:jjwt 0.12.6     (backend/pom.xml:21)
Signing Algorithm: HS256 (Keys.hmacShaKeyFor)      (backend/src/main/java/za/co/csnx/demo/security/JwtService.java:28)
Key Rotation:      not implemented
Revocation:        not supported
Issuer:            demo-backend                     (application.yml:48)
```

**Recommendations:**
1. **Remove the default value** from `application.yml:52`. Crash on startup if `APP_SECURITY_JWT_SECRET` is missing — a refused startup is safer than a known secret.
2. Move secret distribution to a real secret store (Azure Key Vault / GCP Secret Manager — both are already on the deployment roadmap per `.azure/deployment-plan.md` and `.gcp/deployment-plan.md`).
3. Document the docker-compose secret as a `${APP_SECURITY_JWT_SECRET:-?}` template requiring an env var, not a literal value.
4. Consider migrating to RS256 (asymmetric) so verification can be done without trusting verifier code with the signing key.

---

## 5. Brute Force Protection

### 5.1 Account Lockout

**Finding:** [ ] Pass [x] Fail [ ] N/A

**Assessment:**
- [ ] No login-attempt counter
- [ ] No account lockout after N failures
- [ ] No rate limiting on `/api/auth/login` (no `RateLimiter` filter found; no `bucket4j` or similar in `backend/pom.xml`)
- [ ] No CAPTCHA
- [x] `locked` boolean column exists on `app_user` (`AppUser.java:53`) and is honoured by `AppUserDetailsService.java:40`, but no code path *sets* it

**Issues Found:**

| Severity | Issue | Location | Committed By | Approved By | Impact |
|----------|-------|----------|--------------|-------------|--------|
| High | Unlimited login attempts — the `locked` column is checked but never written; no rate-limit filter on `/api/auth/login` | `backend/src/main/java/za/co/csnx/demo/web/AuthController.java:38-46` & no filter in `backend/src/main/java/za/co/csnx/demo/config/SecurityConfig.java` | Unknown | Unknown | Online brute-force / credential-stuffing is unbounded |

**Recommendations:**
- Add a per-IP / per-username rate limiter (e.g. `bucket4j-spring-boot-starter` or a `Filter` backed by a `Caffeine` cache).
- Implement an attempt counter on `AppUser` that increments on `BadCredentialsException`; flip `locked=true` after N failures with auto-unlock after a cool-down.

### 5.2 Credential Stuffing Protection

**Finding:** [ ] Pass [x] Fail [ ] N/A

None of the credential-stuffing controls (anomaly detection, HIBP checks, geographic anomaly detection, user notifications) are implemented. Demo posture: accept the gap given the surface, but document.

---

## 6. Password Reset & Recovery

### 6.1 Password Reset Flow

**Finding:** [ ] Pass [ ] Fail [x] N/A (partial)

The app has a **change-password** flow (user provides current + new) at `/api/auth/change-password` (referenced by `frontend/src/app/auth/AuthProvider.tsx:254`), but no email-based reset for forgotten passwords. The change-password endpoint requires the current password and so does not need a reset-token mechanism.

### 6.2 Account Recovery

**Finding:** [ ] Pass [x] Fail [ ] N/A

No "forgot password" path exists. Locked-out users have no self-service recovery; an admin must clear `locked` and reset their hash via direct DB write.

---

## 7. Single Sign-On (SSO) & SAML

**Finding:** [ ] Pass [ ] Fail [x] N/A

Not implemented.

---

<!-- analysis: manual -->

## 8. Testing Methodology

This section requires manual penetration testing and cannot be completed by automated analysis. Suggested manual tests:

- Replay a captured refresh-cookie cross-origin and confirm CSRF impact.
- Verify that the access JWT signed with the application-default secret matches a token forged offline using the same hardcoded base64 default.
- Confirm absence of rate limiting by scripting 10 000 login attempts and observing no lockout / no throttling.

---

## Summary of Findings

### Critical Issues (Immediate Action Required)
1. **JWT secret has hardcoded dev default in `application.yml`** — token forgery risk if env var is unset.
2. **JWT secret is shipped literally in `docker-compose.yml`** — compose deployments use the dev secret unless the file is hand-edited.

### High Priority Issues
1. **`application-prod.yml` does not override the JWT secret** — falls through to the dev default when env var is missing.
2. **Refresh cookie missing `Secure` + `SameSite`** — vulnerable to interception and CSRF.
3. **Seeded `WCS/wcs/wcs123!` user on every startup** — default credentials in every deployment.
4. **No login rate-limit / lockout** — unlimited credential-stuffing.

### Medium Priority Issues
1. **No server-side password-policy enforcement** — only `@NotBlank`.
2. **Logout does not revoke access token** — leak window of up to 1 h.
3. **No refresh-token rotation** — stolen refresh token usable for 7 days.

---

## Recommendations Summary

### Immediate Actions (0-7 days)
1. Remove the `app.security.jwt.secret` default from `application.yml:52` (let startup fail-fast if unset).
2. Replace the literal secret in `docker-compose.yml:28` with `${APP_SECURITY_JWT_SECRET:?}` and document the local `.env` file pattern.
3. Set `Secure` + `SameSite=Strict` on the refresh cookie at `AuthController.java:82-87` (use `ResponseCookie`).

### Short-term Actions (1-4 weeks)
1. Gate `AppUserSeeder` behind `@Profile("dev")` and document the seed creds as dev-only.
2. Add rate-limiting filter on `/api/auth/login` (`bucket4j-spring-boot-starter`).
3. Implement refresh-token rotation with JTI tracking.

### Long-term Improvements (1-3 months)
1. Wire Azure Key Vault / GCP Secret Manager for `APP_SECURITY_JWT_SECRET` in the deployment templates.
2. Add a server-side JWT blacklist for logout / token revocation.
3. Migrate to RS256 (asymmetric signing) when the second consumer of these tokens appears.

---

## Conclusion

**Authentication Security Posture:** Poor. The application's authentication primitives (bcrypt password hashing, JWT structure, type-discriminated tokens, stateless session policy) are sound, but the **secret-management discipline is weak**: a dev secret is hardcoded in three places (`application.yml`, `docker-compose.yml`, and implicit fall-through in `application-prod.yml`), and operational controls (rate-limiting, lockout, cookie hardening, token revocation) are missing.

**Key Takeaways:**
- Crypto choices are fine; **secret distribution** is the failure mode.
- The seeded default user is a high-severity issue that survives every deployment until manually removed.
- Cookie attributes are an easy, high-impact fix.

**Next Steps:**
1. Apply the three Immediate Actions before any non-dev deployment.
2. Open a tracking ticket for refresh-token rotation and a server-side blacklist.

---

**Assessment completed by:** code-audit security-auditor
**Date:** 2026-05-26
**Review date:** 2026-08-26
