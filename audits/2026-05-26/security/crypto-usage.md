---
genre: security
category: crypto-usage
analysis-type: static
relevance:
  file-patterns:
    - "**/crypto/**"
    - "**/encryption/**"
    - "**/security/**"
  keywords:
    - "encrypt"
    - "decrypt"
    - "hash"
    - "cipher"
    - "aes"
    - "rsa"
    - "hmac"
    - "tls"
    - "ssl"
    - "certificate"
    - "pbkdf2"
  config-keys:
    - "crypto-js"
    - "bcrypt"
    - "node-forge"
    - "tweetnacl"
    - "libsodium"
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# Cryptographic Usage Assessment

**Assessment Date:** 2026-05-26
**Auditor:** code-audit (security-auditor agent)
**Status:** Complete

---

<!-- analysis: static -->

## Executive Summary

**Overall Rating:** [ ] Excellent [ ] Good [x] Fair [ ] Poor [ ] Critical

**Findings:** Critical: 1 | High: 0 | Medium: 1 | Low: 1

The cryptographic primitives in use are modern and well-chosen (bcrypt for passwords, HMAC-SHA256 for JWT signatures via `jjwt` 0.12.6, TLS terminated at the cloud edge). The **key-management hygiene** is the weak link — the JWT signing key has a published default value, which negates the strength of the algorithm.

---

## 1. Inventory

| Use case | Algorithm | Library | Source |
|---|---|---|---|
| Password hashing | bcrypt (cost 10) | Spring Security `BCryptPasswordEncoder` | `backend/src/main/java/za/co/csnx/demo/config/SecurityConfig.java:25` |
| JWT signature | HMAC-SHA256 (HS256) | `io.jsonwebtoken:jjwt 0.12.6` | `backend/src/main/java/za/co/csnx/demo/security/JwtService.java:28-29, 49-56` |
| TLS | Outside scope — terminated at nginx / Azure Container Apps / GCP Cloud Run ingress | n/a | `frontend/nginx.conf.template` / `.azure/deployment-plan.md` |
| Symmetric encryption of data at rest | Not implemented (no `Cipher`, `AES`, `Encryptors` usage in backend) | n/a | — |

No usages of deprecated algorithms (MD5, SHA1 for hashing, DES, RC4) were found in `backend/src/main/java/**`.

---

## 2. Key Findings

### 2.1 JWT key material

**Finding:** [ ] Pass [x] Fail [ ] N/A

| Severity | Issue | Location | Impact |
|---|---|---|---|
| **Critical** | The HMAC signing key is a Base64-encoded constant `dGhpcy1pcy1hLWRldi1zZWNyZXQtcmVwbGFjZS1pbi1wcm9kdWN0aW9uLW9rPw==` (32 ASCII bytes of "this-is-a-dev-secret-replace-in-production-ok?") bound as the default for `APP_SECURITY_JWT_SECRET`. Anyone with read access to the repo can forge any JWT for any subject. | `backend/src/main/resources/application.yml:52` & `docker-compose.yml:28` | Complete authentication bypass via offline token forgery. |
| Medium | Key length is 32 bytes (256 bits) — exactly the HS256 minimum. Acceptable, but a 64-byte key gives margin and is recommended by RFC 7518. | `JwtService.java:28` (decodes via `Base64.getDecoder().decode(props.secret())`) | Minor — algorithm strength is dominated by the secret-distribution problem above. |
| Low | No key rotation strategy — a single static key is used for the lifetime of the deployment. | `JwtProperties.java:7-12` (only one `secret` field) | After a leak, every previously issued token remains forgeable until *every* deployed instance is restarted with the new key. |

### 2.2 Password hashing

**Finding:** [x] Pass [ ] Fail [ ] N/A

bcrypt with per-hash salt (built into bcrypt), 60-char output stored in `app_user.password_hash VARCHAR(255)`. Cost factor is Spring's default of 10 — acceptable; ≥12 is recommended for new deployments on modern hardware.

### 2.3 TLS / transport

Not configured in-app: the backend listens on plain HTTP at `:8080` and relies on the upstream proxy (nginx in compose, Container Apps ingress on Azure, Cloud Run on GCP) for TLS termination. The current `application.yml` has no `server.ssl.*` block — correct for the deployment topology but means **misconfigured operators who skip the proxy expose the app over HTTP**.

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Low | Backend has no `Strict-Transport-Security`, `Content-Security-Policy`, or other security headers configured; relies entirely on the upstream proxy to add them. | `backend/src/main/java/za/co/csnx/demo/config/SecurityConfig.java` (no `headers()` block) | If the proxy is misconfigured or bypassed, no defence-in-depth headers reach the browser. |

---

## 3. Recommendations

### Immediate
- Strip the default value from `app.security.jwt.secret` so startup fails when the env var is unset (see also `authentication.md`).
- Rewrite `docker-compose.yml:28` to require an external `${APP_SECURITY_JWT_SECRET}` (or a project `.env`) rather than ship the secret literally.

### Short-term
- Add `BCryptPasswordEncoder(12)` for new deployments (existing hashes auto-upgrade on next login).
- Add Spring Security headers (`http.headers().contentSecurityPolicy(...)`, `httpStrictTransportSecurity()`) as defence-in-depth.

### Long-term
- Plan for HS256 → RS256 migration so the application instances don't all need to hold the same signing secret.
- Wire JWT key rotation via Key Vault / Secret Manager with overlapping `kid`s.

---

## Conclusion

The crypto primitives are appropriate for the workload; the secret-management posture is the weakness. Treat the JWT-secret Critical as the headline finding for this template.
