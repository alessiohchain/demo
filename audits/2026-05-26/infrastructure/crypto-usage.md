---
genre: infrastructure
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
  config-keys:
    - "crypto-js"
    - "bcrypt"
    - "node-forge"
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# Crypto-Usage Infrastructure Maturity

**Assessment Date:** 2026-05-26
**Status:** Complete

---

<!-- analysis: static -->

## Maturity score — **3 / 5 (Functional)**

Library and algorithm choices are sound: bcrypt for passwords (Spring Security's `BCryptPasswordEncoder`), HS256 via jjwt 0.12.6 for tokens. **Key-distribution is the weak link** — the JWT signing secret has a known default value, which is a cryptographic-control failure independent of the algorithm strength.

| Item | Detail | Source |
|---|---|---|
| Password hash | bcrypt cost 10 | `backend/src/main/java/za/co/csnx/demo/config/SecurityConfig.java:25` |
| JWT signature | HS256 (symmetric) via jjwt | `backend/src/main/java/za/co/csnx/demo/security/JwtService.java:28, 49-56` |
| Random sources | `Instant.now()` for timestamps; JWT lib internal CSPRNG for `iat`/`exp`; no direct `Random` / `SecureRandom` usages in backend code | grep'd `backend/src/main/java/**` |
| TLS termination | Upstream (nginx / Container Apps / Cloud Run) | `frontend/nginx.conf.template`; `.azure/deployment-plan.md`; `.gcp/deployment-plan.md` |
| Symmetric data-at-rest encryption | Not used | — |
| HMAC of arbitrary data | Not used outside JWT | — |
| Library versions | jjwt 0.12.6; Spring Security 6 (managed by Boot 3.5) | `backend/pom.xml:102-117, 47` |

---

## Strengths

- bcrypt is the right default for password storage; library is Spring's well-vetted implementation.
- jjwt 0.12.x is the modern API; the code uses the builder-style `Jwts.builder().signWith(key)` and parser-style `Jwts.parser().verifyWith(key).requireIssuer(...)` — no string-based legacy signing.
- Token type is verified explicitly (`JwtService.java:67-69`) which prevents the classic refresh-as-access mistake.

---

## Gaps

| Severity | Issue | Location | Recommendation |
|---|---|---|---|
| Critical | JWT signing-key has a published default value (see all security audits). | `backend/src/main/resources/application.yml:52`; `docker-compose.yml:28` | Phase-I remediation. |
| Medium | bcrypt cost factor is 10 — Spring's default. For new deployments on modern hardware, 12 is more appropriate. Existing hashes auto-upgrade on next successful login. | `backend/src/main/java/za/co/csnx/demo/config/SecurityConfig.java:25` | `new BCryptPasswordEncoder(12)`. |
| Medium | HS256 means every verifier must hold the signing key. A second consumer (mobile app, partner integration) would mean copying the secret out, which compounds the secret-distribution problem. | `JwtService.java:28` (`Keys.hmacShaKeyFor(...)`) | Plan a migration to RS256 with `kid`-based key rotation. |
| Low | No key rotation strategy. A single static key for the lifetime of the deployment. | `JwtProperties.java:7-12` (only one `secret` field) | Wire Key Vault / Secret Manager with overlapping `kid`s. |
| Low | No security-headers configuration (HSTS, CSP) at the app layer. | `SecurityConfig.java` — no `headers(...)` block | Add Spring's `headers()` configuration as defence-in-depth even though nginx is in front. |
| Info | TLS termination upstream is the correct design for the deployment topologies named — but means a misconfigured operator who bypasses nginx exposes plaintext HTTP. | `nginx.conf.template`; `application.yml` (no `server.ssl.*`) | Document the dependency on the upstream proxy in `docs/`. |

---

## Maturity verdict — **3**

Primitives are excellent; key management is the binding constraint. With the Phase-I secret-management fixes from the security remediation plan, this dimension would lift to 4.
