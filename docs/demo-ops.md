---
title: "Demo App — Operations Guide (Google Cloud)"
subtitle: "CSNX-13935 reference module"
author: "Generated 2026-05-22"
---

# Overview

The `demo` app runs on Google Cloud as three components:

- **Frontend** — nginx + Vite/React, on Cloud Run, publicly reachable over HTTPS. Also proxies `/api/*` to the backend.
- **Backend** — Spring Boot fat-jar, on Cloud Run, **IAM-only** (the public internet gets 403 — only the frontend service account can invoke it).
- **Database** — Cloud SQL for PostgreSQL 16, **private IP only** (no public IP). Reached from the backend via a Serverless VPC Connector; reached from a developer laptop via the Cloud SQL Auth Proxy.

GCP project: **`ai-development-459111`**. Region: **`africa-south1`**.

The full architectural rationale lives in the repo at `.gcp/deployment-plan.md`.

\newpage

# URLs

| What | URL |
|---|---|
| **Frontend (use this)** | <https://demo-frontend-tonnt75awq-bq.a.run.app> |
| Frontend (alt) | <https://demo-frontend-236510297424.africa-south1.run.app> |
| Backend (IAM-only) | <https://demo-backend-tonnt75awq-bq.a.run.app> |

Hitting the backend URL directly returns `403`. That is expected — only the frontend's service account has `roles/run.invoker` on it.

\newpage

# Database access (JDBC)

Cloud SQL has **no public IP**. To connect from a laptop you have to tunnel through the **Cloud SQL Auth Proxy**, which uses your `gcloud` credentials to open an HTTPS tunnel to the instance.

## One-shot setup

There's nothing to install yourself — the script handles it. On first run it downloads `cloud-sql-proxy.exe` into `scripts/tools/`.

## Start the proxy

```powershell
cd C:\software\projects\modules\demo
.\scripts\db-proxy.ps1
```

The script:

1. Downloads the Auth Proxy on first run.
2. Prints the `demo_app` password (pulled live from Secret Manager — never copy-paste into other docs; if it changes, the script will keep printing the current value).
3. Opens a tunnel on `127.0.0.1:5432`.

Leave that window open while you work. **Ctrl+C** closes the tunnel.

If 5432 is taken locally (e.g. `docker-compose up` runs a Postgres on it), pass another port:

```powershell
.\scripts\db-proxy.ps1 -LocalPort 5433
```

## JDBC connection settings

| Field | Value |
|---|---|
| Driver | PostgreSQL (`org.postgresql.Driver`) |
| Host | `127.0.0.1` |
| Port | `5432` (or whatever you passed to `-LocalPort`) |
| Database | `demo` |
| User | `demo_app` |
| Password | printed by the proxy script |
| SSL | off — the proxy is the TLS layer |
| JDBC URL | `jdbc:postgresql://127.0.0.1:5432/demo?sslmode=disable` |

Instance connection name (used by tools that talk to the Auth Proxy directly): `ai-development-459111:africa-south1:demo-pg`.

## Read the password without starting the tunnel

```powershell
gcloud secrets versions access latest --secret=demo-db-password
```

This is the *application* user's password. There is no separate `postgres` admin user in this setup (the demo doesn't need one).

\newpage

# Redeploying code changes

Code changes are rolled to Cloud Run by the deploy script. It does three things: `docker build`, `docker push` to Artifact Registry, and `gcloud run services update` (which creates a new revision).

```powershell
cd C:\software\projects\modules\demo

# Both services
.\scripts\deploy-gcp.ps1

# Just one
.\scripts\deploy-gcp.ps1 backend
.\scripts\deploy-gcp.ps1 frontend
```

**Rules of thumb**

- Java change → `deploy-gcp.ps1 backend` (~2 min).
- React/nginx change → `deploy-gcp.ps1 frontend` (~1 min).
- DB schema change (new Flyway migration in `backend/src/main/resources/db/migration/`) → `deploy-gcp.ps1 backend`. Flyway runs migrations on backend startup before the actuator health probe passes.
- Failed Cloud Run revisions don't take traffic — the old revision keeps serving until the new one passes its startup probe. So a botched deploy can't take the site down; the new revision just won't get traffic.
- Roll back manually if needed: `gcloud run services update-traffic demo-backend --region africa-south1 --to-revisions <PREVIOUS-REVISION>=100`.

# Infrastructure changes (not code)

These go through Terraform, not the deploy script:

- Bumping the Cloud SQL tier
- Changing Cloud Run min/max replicas, CPU, memory
- Adding new secrets
- New IAM roles
- New buckets, queues, etc.

```powershell
cd C:\software\projects\modules\demo\infra\gcp
terraform plan       # review
terraform apply      # apply
```

`infra/gcp/terraform.tfvars` is gitignored (it holds `project_id`). Don't commit it.

The deploy script (`deploy-gcp.ps1`) never touches infrastructure — it only rolls new Cloud Run revisions of services that Terraform already created.

\newpage

# Diagnostics

## Logs

```powershell
# Backend logs (last 100 entries)
gcloud run services logs read demo-backend  --region africa-south1 --limit 100

# Frontend / nginx logs
gcloud run services logs read demo-frontend --region africa-south1 --limit 100

# Filter by severity
gcloud logging read 'resource.type=cloud_run_revision AND resource.labels.service_name="demo-backend" AND severity>=WARNING' --limit 50
```

## Cloud SQL

```powershell
gcloud sql instances describe demo-pg
gcloud sql operations list --instance demo-pg --limit 10
```

## Check what's deployed right now

```powershell
gcloud run services list --region africa-south1
gcloud run revisions list --service demo-backend  --region africa-south1
gcloud run revisions list --service demo-frontend --region africa-south1
```

## Smoke test (after a deploy)

```powershell
$F = 'https://demo-frontend-tonnt75awq-bq.a.run.app'

# SPA loads
curl.exe -s -o NUL -w "HTTP %{http_code}`n" $F/

# /api/me with no token: Spring Security says 403 (proxy + ID token + backend chain works)
curl.exe -s -o NUL -w "HTTP %{http_code}`n" $F/api/me

# Backend directly: Cloud Run IAM says 403 (only frontend SA can invoke)
curl.exe -s -o NUL -w "HTTP %{http_code}`n" https://demo-backend-tonnt75awq-bq.a.run.app/api/me
```

\newpage

# Cost & lifecycle

Approximate monthly cost while idle: **\$30–40 USD**. Largest items: Cloud SQL Burstable instance (~\$11), 2 × Cloud Run min-1 instances (~\$10–15), Serverless VPC Connector (~\$9).

Cloud Run scales 1 → 3 replicas under load. No scale-to-zero (a JVM cold start would be a poor demo experience).

**To tear it all down:**

```powershell
cd C:\software\projects\modules\demo\infra\gcp
terraform destroy
```

This removes everything except the project itself and the API enablements (those are kept on purpose — they're free, and re-enabling takes ~2 min per API).

# File layout (for reference)

```
demo/
  .gcp/deployment-plan.md          # Architecture rationale
  infra/gcp/                       # Terraform: VPC, SQL, secrets, Cloud Run, IAM
    terraform.tfvars               # project_id (gitignored)
  scripts/
    deploy-gcp.ps1                 # Build + push + roll Cloud Run
    db-proxy.ps1                   # Local JDBC tunnel via Cloud SQL Auth Proxy
    tools/cloud-sql-proxy.exe      # Downloaded on first proxy run
    README.md
  backend/                         # Spring Boot
  frontend/                        # React + Vite + nginx
    nginx.conf.template            # envsubst-rendered at container start
    docker-entrypoint.sh           # Renders template, fetches ID token
  docker-compose.yml               # Local 3-tier dev stack (still works)
  docs/
    demo-ops.md                    # Source for this Word doc
    demo-ops.docx                  # This document
```
