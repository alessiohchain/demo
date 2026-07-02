# Scripts

| Script | What it does |
|---|---|
| `stack.ps1`      | Local container lifecycle: up / down / stop / start / restart / status / logs |
| `redeploy.ps1`   | Rebuild this repo from latest `main` (backend jar + images) and restart its containers |
| `deploy-gcp.ps1` | Build + push + roll Cloud Run revisions for backend, frontend, or both |
| `deploy-azure.ps1` | Build + push + roll Container Apps revisions for backend, frontend, or both |
| `db-proxy.ps1`   | Open a local JDBC port to the private-IP Cloud SQL via Cloud SQL Auth Proxy |

## Local dev: rebuild & restart containers

```powershell
# --- container lifecycle (no rebuild) ---
.\scripts\stack.ps1 -Action up           # start the stack (postgres + backend + frontend)
.\scripts\stack.ps1 -Action status       # docker compose ps
.\scripts\stack.ps1 -Action logs -Service backend -Follow
.\scripts\stack.ps1 -Action down         # stop + remove containers (keeps the DB volume)

# --- rebuild from latest main and restart updated containers ---
.\scripts\redeploy.ps1                    # git pull main -> mvnw package -> compose up -d --build
.\scripts\redeploy.ps1 -NoPull -Service frontend   # frontend-only rebuild of the current checkout
```

`redeploy.ps1` builds the backend fat-jar on the host first (the backend
Dockerfile COPYs a pre-built `target/*-backend-*.jar`) and auto-resolves a
Temurin **JDK 21** (the machine default `JAVA_HOME` is JDK 8).

**Shared-engine changes** (the `@alessiohchain/csnx-engine` npm package or the
`csnx-engine-spring` / `csnx-engine-ai` maven artifacts) are owned by the
**platform** repo. Build + propagate them from there first, then redeploy here:

```powershell
# in the platform repo
.\scripts\build-shared.ps1 -Component engine      # bump + re-vendor into every module
# then, back here
.\scripts\redeploy.ps1 -NoPull -Service frontend
```

## Redeploy after a code change

```powershell
# both services
.\scripts\deploy-gcp.ps1

# backend only (after a Java/pom change)
.\scripts\deploy-gcp.ps1 backend

# frontend only (after a React/nginx change)
.\scripts\deploy-gcp.ps1 frontend
```

The script does not touch infrastructure. If you change `infra/gcp/*.tf`, run
`terraform apply` in `infra/gcp` instead.

The Azure sibling works the same way (`GH_PACKAGES_TOKEN` must be set for
frontend builds; infra changes go through `terraform apply` in `infra/azure`):

```powershell
.\scripts\deploy-azure.ps1            # both services
.\scripts\deploy-azure.ps1 backend    # backend only
.\scripts\deploy-azure.ps1 frontend   # frontend only
```

## Connect a JDBC client (DBeaver, IntelliJ, psql, etc.)

```powershell
.\scripts\db-proxy.ps1
```

First run downloads `cloud-sql-proxy.exe` into `scripts/tools/`. It then prints
the `demo_app` password and starts a tunnel on `127.0.0.1:5432`. Connect with:

```
jdbc:postgresql://127.0.0.1:5432/demo?sslmode=disable
user:     demo_app
password: (printed by the script)
```

Ctrl+C closes the tunnel.

If 5432 is already taken locally (e.g. you run `docker-compose up` with
Postgres on it), pass a different port:

```powershell
.\scripts\db-proxy.ps1 -LocalPort 5433
```
