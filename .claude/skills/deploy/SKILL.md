---
name: deploy
description: Stop, start, restart, and deploy the Demo module's local Docker stack. Use when the user asks to start/stop/restart the containers, redeploy Demo after a code change, ship/deploy changes locally, check container status, or tail container logs. Wraps scripts/stack.ps1 and scripts/redeploy.ps1.
tools: Bash, Read
---

# Deploy (Demo)

Drive the local 3-container stack (`postgres` + `backend` + `frontend`) for the
Demo module via the PowerShell helpers in `scripts/`. Pick the action from the
user's words, run the matching command, then report `docker compose ps`.

All commands run from the repo root with:
`powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/<script>.ps1 <args>`

Use **forward slashes** in the script path. These commands run through the Bash
tool, which eats a backslash (a back-slashed `scripts\redeploy.ps1` collapses to
`scriptsredeploy.ps1`). PowerShell's `-File` accepts `/` on Windows, so a
forward-slash path survives the shell and resolves correctly.

## Actions

| User intent | Command |
|---|---|
| **deploy / ship changes** (rebuild from latest main + restart) | `scripts/redeploy.ps1` |
| deploy current checkout (no git pull) | `scripts/redeploy.ps1 -NoPull` |
| frontend-only deploy | `scripts/redeploy.ps1 -NoPull -Service frontend` |
| backend-only deploy | `scripts/redeploy.ps1 -NoPull -Service backend` |
| **start** the stack | `scripts/stack.ps1 -Action up` |
| **stop** (keep containers) | `scripts/stack.ps1 -Action stop` |
| **restart** | `scripts/stack.ps1 -Action restart` |
| **status** | `scripts/stack.ps1 -Action status` |
| **logs** (add `-Service backend` / `-Follow`) | `scripts/stack.ps1 -Action logs` |
| **tear down** (remove containers, keep DB volume) | `scripts/stack.ps1 -Action down` |

`-Service` takes `backend` \| `frontend` \| `postgres`.

## What `redeploy.ps1` does (and why it's needed)

1. fast-forwards the checkout to `origin/main` (skip with `-NoPull`),
2. builds the backend fat-jar on the host with **JDK 21** — required because the
   backend Dockerfile COPYs a pre-built `target/demo-backend-*.jar`,
3. `docker compose up -d --build` — the frontend image re-runs `npm ci` against
   the vendored engine and rebuilds the Vite bundle, so this is the **only** way
   UI changes reach the running container (it serves the bundle baked at
   image-build time),
4. prints `docker compose ps`.

The script auto-resolves the Temurin JDK 21 (the machine default `JAVA_HOME` is
JDK 8), so you do not need to set it.

## Shared-engine changes

If the change is in the shared engine (`@alessiohchain/csnx-engine` npm package
or the `csnx-engine-spring` / `csnx-engine-ai` maven artifacts), those are owned
by the **platform** repo — build + propagate them there first
(`platform/scripts/build-shared.ps1`), then run `scripts/redeploy.ps1` here.

## Rules

- **Confirm before `down`** (it removes containers) and before any deploy that
  pulls main onto a dirty tree — surface what will happen first.
- After any action, run `scripts/stack.ps1 -Action status` (or rely on
  redeploy's trailing `ps`) and report which containers are healthy + their
  host ports (backend 8092, frontend 8082, postgres 5434).
- If a deploy fails, show the failing step's output; do not silently retry.
- These scripts manage the **local Docker stack only** — for GCP/Cloud Run use
  `scripts/deploy-gcp.ps1`.
