---
name: deploy
description: Stop, start, restart, and deploy the Demo module's local Docker stack. Use when the user asks to start/stop/restart the containers, redeploy Demo after a code change, ship/deploy changes locally, check container status, or tail container logs. Wraps scripts/stack.ps1 and scripts/redeploy.ps1.
tools: Bash, Read
---

# Deploy (Demo)

Drive the local 3-container stack (`postgres` + `backend` + `frontend`) via the
PowerShell helpers in `scripts/`. Pick the action from the user's words, run the
matching command, then report `docker compose ps`.

The scripts are the fleet's shared `stack.ps1` / `redeploy.ps1`; what they do
and why (host-built jar, frontend image rebuild, JDK 21 auto-resolve) is
explained once in the platform repo's deploy skill
(`../platform/.claude/skills/deploy/SKILL.md`). Run every command from the repo
root with **forward slashes** (the Bash tool strips backslashes):
`powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/<script>.ps1 <args>`

## Actions

| User intent | Command |
|---|---|
| **deploy / ship changes** (fast-forward to `origin/main`, rebuild, restart) | `scripts/redeploy.ps1` |
| deploy current checkout (no git pull) | `scripts/redeploy.ps1 -NoPull` |
| frontend-only deploy | `scripts/redeploy.ps1 -NoPull -Service frontend` |
| backend-only deploy | `scripts/redeploy.ps1 -NoPull -Service backend` |
| redeploy reusing the jar already in `backend/target/` | `scripts/redeploy.ps1 -NoPull -NoJar` |
| **start** the stack | `scripts/stack.ps1 -Action up` |
| start against a per-environment file | `scripts/stack.ps1 -Action up -Environment local\|test\|preprod\|prod` |
| **stop** (keep containers) | `scripts/stack.ps1 -Action stop` |
| **resume** stopped containers | `scripts/stack.ps1 -Action start` |
| **restart** | `scripts/stack.ps1 -Action restart` |
| **status** | `scripts/stack.ps1 -Action status` |
| **logs** (add `-Service backend` / `-Follow`) | `scripts/stack.ps1 -Action logs` |
| **tear down** (remove containers, keep DB volume) | `scripts/stack.ps1 -Action down` |

`-Service` takes `postgres` \| `backend` \| `frontend` on `stack.ps1`, and only
`backend` \| `frontend` on `redeploy.ps1`.

## This module's values

- **Jar**: `backend/target/demo-backend-*.jar` (the Dockerfile COPYs it, so a
  stale jar ships unless `redeploy.ps1` builds it).
- **Trunk**: `main`.
- **Prerequisite**: the platform stack must be up — IdP `:8090`, portal `:8091`,
  RabbitMQ `:5672` (demo consumes and produces events through it).
- **Postgres image**: built locally from `infra/postgres-wal2json` (WAL CDC);
  moving an old `postgres:16-alpine` volume to it needs the volume recreated once
  (see the note in `docker-compose.yml`).
- **Engine source**: the npm engine comes from GitHub Packages (the frontend
  image needs `GH_PACKAGES_TOKEN`, passed as the `gh_token` build secret); the
  Maven engines are vendored in `backend/vendor-repo`. An engine change is made
  and released in the platform repo and reaches here only by re-vendoring
  (`scripts/vendor-engine.ps1`, procedure in
  `../platform/docs/engine-versioning.md` §Consumer — re-vendoring); then redeploy.

## Rules

- **Confirm before `down`** (it removes containers) and before any deploy that
  pulls main onto a dirty tree — surface what will happen first.
- After any action, report which containers are healthy and their host ports:
  backend **8092**, frontend **8085**, postgres **5435**.
- If a deploy fails, show the failing step's output; do not silently retry.
- These scripts manage the **local Docker stack only** — Azure:
  `scripts/deploy-azure.ps1` (infra: `scripts/deploy-infra-azure.ps1`); GCP/Cloud
  Run: `scripts/deploy-gcp.ps1` (`docs/demo-ops.md`).
