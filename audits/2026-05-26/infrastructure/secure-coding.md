---
genre: infrastructure
category: secure-coding
analysis-type: static
relevance:
  file-patterns:
    - "**/src/**"
    - "**/lib/**"
    - "**/app/**"
  keywords:
    - "lint"
    - "eslint"
    - "sonarqube"
    - "static-analysis"
    - "code-review"
  config-keys:
    - "eslint"
    - "prettier"
    - "@typescript-eslint"
    - "sonarqube"
  always-include: true
severity-scale: "Critical|High|Medium|Low|Info"
---

# Secure-Coding Infrastructure Maturity

**Assessment Date:** 2026-05-26
**Status:** Complete

---

<!-- analysis: static -->

## Maturity score — **2 / 5 (Outdated)**

There is **no enforced static-analysis gate**: no SAST, no SonarQube, no Spotbugs, no `dependency-check-maven`, no Checkstyle / PMD on the Java side. The frontend has ESLint but with only the React-Hooks + TS-recommended rule sets — no security-focused plugins. CI does not exist to enforce any of this.

| Tool | Configured? | Source |
|---|---|---|
| **Backend Java** | | |
| Checkstyle / PMD / SpotBugs | No | `backend/pom.xml` — no plugin entries |
| OWASP Dependency-Check | No | (same) |
| SonarQube | No | (same) |
| ErrorProne / NullAway | No | (same) |
| JaCoCo (coverage gate) | No | (same) |
| **Frontend TS** | | |
| ESLint | Yes (minimal rules) | `frontend/eslint.config.js` |
| Prettier | No | (not in `package.json`) |
| eslint-plugin-security | No | (not in `package.json`) |
| eslint-plugin-jsx-a11y | No | (not in `package.json`) |
| TypeScript strict | Yes (per `CLAUDE.md:108-109`) | `frontend/tsconfig.app.json` |
| Bundle / unused-export detection (`knip`, `ts-prune`) | No | (not in `package.json`) |
| **Cross-cutting** | | |
| Pre-commit hooks (husky / lefthook) | No | (no config files in repo root) |
| CI pipeline | No | (no `.github/`, `.gitlab-ci.yml`, etc.) |
| Commit-message linting (commitlint) | No | — |

---

## Convention enforcement

`CLAUDE.md` documents the conventions thoroughly (lines 65-160), including "no Lombok", "no `space-y-*` Tailwind", "use shadcn primitives via CLI", "no raw colour utilities for status", etc. These rely on **human review** — none of them are enforced by tooling.

| Severity | Issue | Location | Recommendation |
|---|---|---|---|
| High | None of the conventions in `CLAUDE.md:65-160` are mechanically enforced. A future contributor (or another module cloning this template) will diverge silently. | `CLAUDE.md`; absence of ESLint custom rules / Checkstyle config | Convert the most-load-bearing conventions (`no space-y-*`, `no raw colour utilities`, `space-y- → gap-`) into ESLint rules. |
| High | No SAST / dependency-vulnerability scanner. | (no plugins in `backend/pom.xml`; no `npm audit` step) | Wire `dependency-check-maven`, `spotbugs-maven-plugin`, and `npm audit` into the CI workflow you'll add for `infrastructure.md`. |
| Medium | ESLint config is minimal: only `js.configs.recommended`, `typescript-eslint.configs.recommended`, `react-hooks`, and `react-refresh`. No security or a11y rules. | `frontend/eslint.config.js:11-25` | Add `eslint-plugin-security`, `eslint-plugin-jsx-a11y`, `eslint-plugin-react`. |
| Medium | No Prettier config. Formatting is implicit. | (absent) | Add `prettier` + a CI check. |
| Low | No pre-commit hook framework. | (absent) | Add `lefthook` or `husky` running ESLint on staged files. |

---

## Static evidence the code is reasonably clean

Even without enforced tooling:
- TS strict (`CLAUDE.md:108-109`) means `any`-typed paths are rare. None spotted in the read sample.
- No `// TODO`, `FIXME`, or `HACK` markers found in the read sample (a sweep of the file-by-file reads in this audit returned none).
- The Spring Boot starters consistently exclude `spring-boot-starter-logging` (`backend/pom.xml`) — disciplined.
- `BaseEntity` / `BaseRepository` abstractions are clean and re-used (`backend/src/main/java/za/co/csnx/demo/common/`).
- `GlobalExceptionHandler` covers every exception class explicitly; no `try/catch` swallowing in the read sample.
- Frontend uses controlled inputs throughout; no `dangerouslySetInnerHTML`.

This says the *code* is decent — the *process* is what's missing.

---

## Maturity verdict — **2**

Code quality looks good by inspection; the absence of any automated gate is the problem. With even a minimal CI workflow running ESLint, `mvnw verify`, and `npm audit`, this lifts to 3. Adding a SAST / dependency-check tool lifts to 4.
