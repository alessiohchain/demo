---
genre: security
category: infrastructure
analysis-type: static
relevance:
  file-patterns:
    - "**/docker*"
    - "**/k8s/**"
    - "**/kubernetes/**"
    - "**/terraform/**"
    - "**/helm/**"
    - "docker-compose*"
  keywords:
    - "docker"
    - "kubernetes"
    - "container"
    - "helm"
    - "terraform"
    - "nginx"
    - "apache"
    - "proxy"
    - "firewall"
  config-keys: []
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# Infrastructure (Security View)

**Assessment Date:** 2026-05-26
**Auditor:** code-audit (security-auditor agent)
**Status:** Complete

---

<!-- analysis: static -->

## Executive Summary

**Overall Rating:** [ ] Excellent [ ] Good [x] Fair [ ] Poor [ ] Critical

**Findings:** Critical: 1 | High: 1 | Medium: 3 | Low: 2

Containerised three-tier stack (postgres, backend, frontend-nginx) wired by `docker-compose.yml`. The backend Dockerfile is well-structured (multi-stage, non-root user). The compose configuration ships hardcoded secrets and exposes the database port. The frontend nginx config does deliberate Origin-stripping for cross-cloud serverless deployment — clever but worth flagging as a CORS-bypass artefact.

---

## 1. Topology

```
host:8081 ─→ frontend (nginx:1.27-alpine)
              │  /api/ proxy_pass → ${BACKEND_URL}
              ▼
host:8080 ─→ backend  (eclipse-temurin:21-jre-alpine, non-root user `app`)
              │
              ▼
host:5432 ─→ postgres (postgres:16-alpine)
```

Infrastructure-as-code: `infra/gcp/` (Terraform, not deeply reviewed), `.azure/deployment-plan.md` (draft Bicep plan), `.gcp/deployment-plan.md` (planning doc).

---

## 2. Findings

### 2.1 Critical: dev JWT secret committed in compose

| Severity | Issue | Location | Impact |
|---|---|---|---|
| **Critical** | `APP_SECURITY_JWT_SECRET` is set to the published dev value `dGhpcy1pcy1hLWRldi1zZWNyZXQtcmVwbGFjZS1pbi1wcm9kdWN0aW9uLW9rPw==` in compose. Combined with the same default in `application.yml:52`, any compose-based deployment runs with a known signing key. | `docker-compose.yml:28` | Token forgery against every compose-deployed environment. See `authentication.md` and `crypto-usage.md`. |

### 2.2 Hardcoded Postgres credentials

| Severity | Issue | Location | Impact |
|---|---|---|---|
| High | `POSTGRES_USER=demo`, `POSTGRES_PASSWORD=demo`, and matching `DB_USER`/`DB_PASSWORD` on the backend service. | `docker-compose.yml:5-7, 26-27` | See `database.md`. |

### 2.3 Postgres port published to the host

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Medium | `5432:5432` mapping makes the DB reachable from anything that can reach the host. | `docker-compose.yml:15-16` | Lateral movement risk. |

### 2.4 No container hardening flags

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Medium | None of the three services declare `read_only: true`, `security_opt: ["no-new-privileges:true"]`, `cap_drop: [ALL]`, or `tmpfs` mounts. | `docker-compose.yml` (all services) | Default Docker container privileges; a compromised process can write to the FS, escalate caps, etc. |
| Low | Frontend Dockerfile runs nginx as root (no `USER` directive before `ENTRYPOINT`). Backend Dockerfile correctly uses non-root `app:app` (line 21). | `frontend/Dockerfile:10-18` | Root-running nginx in a container is the default but increases blast radius on container breakout. |

### 2.5 Nginx strips `Origin` to bypass backend CORS

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Medium | The nginx template sets `proxy_set_header Origin "";` on every `/api/` proxy hop. The inline comment explains it: the prod CORS allowedOrigins list excludes the Cloud Run frontend URL, so Spring would reject the request — stripping the Origin makes the backend treat the call as same-origin. This is a deliberate workaround but it **defeats Spring's CORS protection** on the proxy path: any browser that lands on the frontend domain implicitly trusts the proxy. | `frontend/nginx.conf.template:14-19` | If the frontend domain itself is ever served untrusted content (XSS, third-party widget), the backend has no CORS-layer defence — the proxied calls look like same-origin. The actual CORS protection becomes "does the attacker control content on the frontend domain". |

### 2.6 Frontend GCP-specific metadata-server calls

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Low | `frontend/docker-entrypoint.sh:31-36` fetches an ID token from `http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/identity?audience=${BACKEND_AUDIENCE}`. Token refresh loop sleeps for 1800 s and reloads nginx. The fetched token is injected as `X-Serverless-Authorization: Bearer <id-token>` via `proxy_set_header` (line 23). The `sed`-based redaction at line 66 only masks the token inside the rendered nginx config dump — the token itself sits in process memory and in the rendered `/etc/nginx/conf.d/default.conf`. | `frontend/docker-entrypoint.sh:31-66` | Token is correctly fetched from the instance metadata service (no static creds) and refreshed regularly. Risk is low; flag is just that anyone who can `exec` into the frontend container can read the current ID token from the rendered config file. |

### 2.7 Healthcheck endpoint exposes plaintext to internal network

| Severity | Status | Notes |
|---|---|---|
| Pass | Backend health check uses `actuator/health` over plain HTTP within the compose network — acceptable. The endpoint returns only `UP` (not the privileged details, since `show-details: when-authorized`). |

---

## 3. Recommendations

### Immediate
- Replace the literal JWT secret in `docker-compose.yml:28` with `${APP_SECURITY_JWT_SECRET:?}`.
- Replace literal DB credentials with `${POSTGRES_PASSWORD:?}`.
- Bind Postgres only to `127.0.0.1:5432` or drop the port mapping entirely.

### Short-term
- Add `security_opt: ["no-new-privileges:true"]`, `read_only: true` (with tmpfs for `/tmp`), and `cap_drop: [ALL]` (`cap_add` only what's needed) on each compose service.
- Add a `USER nginx` line to `frontend/Dockerfile` (the upstream nginx images include the `nginx` user).
- Replace the `Origin: ""` strip in `nginx.conf.template:19` with an explicit `proxy_set_header Origin https://internal-backend.example.com;` so the backend's CORS layer keeps doing its job.

### Long-term
- Wire managed identity (Azure) / Workload Identity (GCP) for the backend → DB connection so passwords are never in env vars at all.
- Adopt `docker scout` / `trivy` scans in CI for every image build.

---

## Conclusion

The container/infra security posture is **fair** for a demo but would be **poor** if shipped as-is to a customer-facing environment. The single Critical (committed JWT secret) is also flagged in `authentication.md` and `crypto-usage.md` — it is the headline finding of this audit.
