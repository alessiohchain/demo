---
genre: infrastructure
category: front-end
analysis-type: static
relevance:
  file-patterns:
    - "**/src/**"
    - "**/components/**"
    - "**/pages/**"
    - "**/public/**"
  keywords:
    - "react"
    - "vue"
    - "angular"
    - "svelte"
    - "webpack"
    - "vite"
    - "typescript"
  config-keys:
    - "react"
    - "vue"
    - "@angular/core"
    - "svelte"
    - "next"
    - "nuxt"
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# Front-End Infrastructure Maturity

**Assessment Date:** 2026-05-26
**Auditor:** code-audit (infrastructure-auditor agent)
**Status:** Complete

---

<!-- analysis: static -->

## Maturity score — **3 / 5 (Functional / Some gaps)**

Modern technical choices (Vite 8, React 19, TS strict, TanStack Query, shadcn primitives) but **zero test coverage** on ~90 TS/TSX source files is the binding constraint. The frontend would be Level 4 with even baseline Vitest tests.

| Rubric level | Match |
|---|---|
| 5 — Excellent | No |
| 4 — Modern | No — test gap drops one level |
| **3 — Functional** | **Yes — modern stack, idiomatic patterns, but missing tests + CI** |

---

## 1. Technology

| Layer | Choice | Version | Source |
|---|---|---|---|
| Framework | React | 19.2.6 | `frontend/package.json:35` |
| Language | TypeScript (strict) | 5.9.3 | `frontend/package.json:58`; `frontend/tsconfig.app.json` |
| Build tool | Vite | 8.0.13 | `frontend/package.json:60` |
| Router | react-router-dom | 7.15.1 | `frontend/package.json:39` |
| State (server) | TanStack Query | 5.100.10 | `frontend/package.json:28` |
| State (table) | TanStack Table | 8.21.3 | `frontend/package.json:29` |
| HTTP | axios | 1.16.1 | `frontend/package.json:30` |
| Forms | react-hook-form + zod + @hookform/resolvers | 7.76.0 / 4.4.3 / 5.2.2 | `frontend/package.json:38, 43, 13` |
| UI primitives | @radix-ui/* (15 packages) — shadcn-style | various | `frontend/package.json:14-27` |
| Styling | Tailwind CSS | 4.3.0 | `frontend/package.json:57` |
| Icons | lucide-react | 1.16.0 | `frontend/package.json:34` |
| Date picker | react-day-picker | 10.0.1 | `frontend/package.json:37` |
| Toasts | sonner | 2.0.7 | `frontend/package.json:40` |
| Linting | ESLint + typescript-eslint + react-hooks + react-refresh | 9.13 / 8.11 | `frontend/package.json:46-59`; `frontend/eslint.config.js` |
| Test framework | **None** | — | (no vitest / jest / playwright in `package.json`) |
| Container | nginx multi-stage Dockerfile | — | `frontend/Dockerfile` |

---

## 2. Architecture

```
frontend/src/
├── app/                       Shell, auth provider, router, theme, error bus, urlFlags
│   ├── api/client.ts          axios instance, interceptors, refresh-on-401
│   ├── auth/AuthProvider.tsx  context + login/logout/changePassword/switchFacility
│   └── error/MultiErrorDialog Etc.
├── components/ui/             shadcn-installed primitives (button, dialog, input, label, card, table, etc.)
├── engine/                    Metadata-engine scaffolding (breadcrumbs, dialog, filter, form, grid, process, screen, toolbar)
├── shell/                     Top-level shell screens (Login, AppShell, Sidebar, Landing, ChangePassword, FastpathInput, NetworkActivityBar, WarehouseSwitcher)
├── lib/                       Utilities + queryClient
└── main.tsx                   Bootstrap: QueryClient, AuthProvider, ThemeProvider, RouterProvider
```

**Strengths:**
- Clear feature folders. `engine/` is intentionally scoped for the metadata-driven UI roadmap (`CLAUDE.md:195-203`).
- Path alias `@/` configured via Vite + tsconfig (`frontend/vite.config.ts:7-11`).
- shadcn primitives tracked upstream via `components.json` — convention enforced in `CLAUDE.md:124-138`.
- Single API client (`app/api/client.ts`) with bearer + auto-refresh-on-401 interceptor.
- TanStack Query for server state (no API responses stored in component state, per `CLAUDE.md:98-99`).
- Auth token in memory only (not `localStorage`) — good security posture.
- `withCredentials: true` + httpOnly refresh cookie — modern auth pattern.

**Gaps:**
- The `engine/` folder is partially-built scaffolding (process, parentGridStore, workflowStateStore, breadcrumbStore, etc.) for a future metadata-driven UI that the demo doesn't fully exercise. Reading `main.tsx:11-18` shows 8 separate clear-functions imported — significant pre-built infrastructure with limited current use.

---

## 3. Build, lint, tooling

| Item | State | Source |
|---|---|---|
| Build | `tsc -b && vite build` | `frontend/package.json:8` |
| Dev server | Vite, port 5173, `/api` proxied to `:8080` | `frontend/vite.config.ts:12-20` |
| Lockfile | `package-lock.json` present | `frontend/package-lock.json` |
| Lint | `eslint .` | `frontend/package.json:9`; `frontend/eslint.config.js` |
| Lint rules | recommended JS, recommended TS, react-hooks, react-refresh | `frontend/eslint.config.js:11-25` |
| Format | (no prettier visible) | — |
| Type check | TS strict (via `tsc -b` in build) | `frontend/tsconfig.app.json` (not read in detail; convention per `CLAUDE.md:108-109`) |
| Container | Multi-stage Dockerfile: `node:22-alpine` → `nginx:1.27-alpine`; uses `npm ci` | `frontend/Dockerfile` |
| nginx config | Template + entrypoint script with optional GCP metadata-server ID-token injection | `frontend/nginx.conf.template`, `frontend/docker-entrypoint.sh` |

**Strengths:**
- `npm ci` in Docker = reproducible installs from lockfile.
- Multi-stage build keeps the final image at `nginx:1.27-alpine` size.
- Entrypoint script is reusable across local-compose / Azure / GCP — `BACKEND_URL` + optional `BACKEND_AUDIENCE` parameterise the same image.

**Gaps:**
- No Prettier config detected.
- No `eslint-plugin-jsx-a11y` (see `accessibility.md`).
- No `eslint-plugin-security` (modest gap).
- ESLint `ecmaVersion: 2020` (line 13) — could safely be `2024` for the React 19 + Vite 8 era.

---

## 4. Testing

**None.** No `vitest`, `jest`, `@testing-library/react`, `playwright`, `cypress`, or `msw` in `package.json`. No `*.test.ts` / `*.spec.tsx` files in `src/`. The frontend ships with **0 % test coverage**.

The frontend's only static-analysis gate is `npm run lint`. No type-coverage tool. No visual-regression test.

---

## 5. Findings

| Severity | Issue | Location | Recommendation |
|---|---|---|---|
| High | No frontend test infrastructure at all. ~90 TS/TSX source files, 0 tests. | `frontend/package.json` (no test framework) | Add Vitest + @testing-library/react; cover `AuthProvider`, `api/client.ts` interceptors, and the Login form as first pass. |
| Medium | No CI runs the lint/build/test pipeline. | (no `.github/workflows/`) | Add a workflow: `npm ci && npm run lint && npm run build`. |
| Medium | Documentation drift — `CLAUDE.md:41` says "React 18 … Tailwind 3" but `package.json` is React 19 + Tailwind 4. | `CLAUDE.md:41` vs `frontend/package.json:35, 57` | Update CLAUDE.md (it's the convention-of-record). |
| Low | No Prettier / formatter config. | (absent) | Add `prettier` + a CI check. |
| Low | ESLint uses `ecmaVersion: 2020`. | `frontend/eslint.config.js:13` | Bump to 2024. |
| Info | `engine/` folder contains scaffolding for a metadata-driven UI not yet wired into the demo screens. Pre-built optionality, not a defect. | `frontend/src/engine/` | Document the maturity (placeholder vs. live) in `docs/architecture.md`. |

---

## Maturity verdict — **3**

Modern stack and idiomatic patterns put this *near* Level 4, but the absence of a test suite and CI plus the slight doc drift hold it at Level 3 (Functional). Adding Vitest with even a handful of tests would lift this to 4.
