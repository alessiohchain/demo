---
genre: security
category: third-party-dependencies
analysis-type: static
relevance:
  file-patterns:
    - "package.json"
    - "package-lock.json"
    - "go.mod"
    - "requirements.txt"
    - "Gemfile"
    - "pom.xml"
    - "Cargo.toml"
  keywords:
    - "dependency"
    - "package"
    - "vulnerability"
    - "cve"
    - "audit"
    - "npm"
    - "pip"
    - "cargo"
  config-keys: []
  always-include: true
severity-scale: "Critical|High|Medium|Low|Info"
---

# Third-Party Dependency Assessment

**Assessment Date:** 2026-05-26
**Auditor:** code-audit (security-auditor agent)
**Status:** Complete

---

<!-- analysis: static -->

## Executive Summary

**Overall Rating:** [ ] Excellent [x] Good [ ] Fair [ ] Poor [ ] Critical

**Findings:** Critical: 0 | High: 0 | Medium: 2 | Low: 3

Dependency hygiene is reasonable: a recent Spring Boot 3.5.14 parent pulls vetted versions for most things; jjwt 0.12.6 and springdoc 2.8.13 are pinned to recent releases; the frontend tracks current major React/Vite/TanStack-Query. The standout issues are the **absence of a vulnerability scanner in CI** (no CI exists at all) and the use of `^` ranges on every frontend dep.

---

## 1. Inventory — Backend (`backend/pom.xml`)

| Group | Artifact | Version | Source | Notes |
|---|---|---|---|---|
| `org.springframework.boot` | spring-boot-starter-parent | **3.5.14** | line 8 | Recent (Boot 3.5 line); receives 3.5.x security updates. |
| `org.springframework.boot` | starter-actuator | (BOM) | line 26 | Logging starter excluded — good (logback → log4j2 switch). |
| `org.springframework.boot` | starter-data-jpa | (BOM) | line 37 | |
| `org.springframework.boot` | starter-security | (BOM) | line 47 | |
| `org.springframework.boot` | starter-validation | (BOM) | line 57 | |
| `org.springframework.boot` | starter-web | (BOM) | line 67 | |
| `org.springframework.boot` | starter-log4j2 | (BOM) | line 77 | Replaces the default Logback starter. |
| `org.flywaydb` | flyway-core, flyway-database-postgresql | (BOM) | line 81-87 | |
| `org.springdoc` | springdoc-openapi-starter-webmvc-ui | **2.8.13** | line 91 | Up-to-date (springdoc 2.x is current). |
| `org.mapstruct` | mapstruct + processor | **1.6.3** | line 97, 171 | Up-to-date. |
| `io.jsonwebtoken` | jjwt-api / jjwt-impl / jjwt-jackson | **0.12.6** | line 102-117 | Up-to-date (0.12.x is current for jjwt). |
| `org.springframework.boot` | spring-boot-devtools | (BOM) | line 121 | `runtime`/`optional`; shouldn't ship in prod jar. |
| `org.postgresql` | postgresql | (BOM) | line 127 | |
| `org.testcontainers` | junit-jupiter, postgresql | (BOM) | line 147-154 | Test scope. |

### Backend findings

| Severity | Issue | Location | Notes |
|---|---|---|---|
| Medium | No dependency-vulnerability scanner integrated. No `dependency-check-maven`, no `snyk-maven-plugin`, no GitHub Dependabot configuration. CI is also absent (see `infrastructure` audit), so even Spring Boot 3.5.14 → 3.5.15 will not propagate automatically. | `backend/pom.xml` (no scanner plugin); `.github/` absent | Vulnerable transitives will land silently. |
| Low | `spring-boot-devtools` is included with `<optional>true</optional>` and `<scope>runtime</scope>`. Boot's repackager strips it from the production fat jar, **but** the custom Dockerfile at `backend/Dockerfile:18-20` copies `BOOT-INF/lib` directly, not the repackaged jar — confirm devtools is excluded from the runtime classpath. | `backend/pom.xml:120-124`; `backend/Dockerfile:11-20` | If devtools ships in prod, the LiveReload server listens on `:35729` and the H2-console may be reachable. Manual verification required. |

---

## 2. Inventory — Frontend (`frontend/package.json`)

| Group | Notable versions | Notes |
|---|---|---|
| Core | `react ^19.2.6`, `react-dom ^19.2.6`, `react-router-dom ^7.15.1` | React 19 LTS line. |
| State | `@tanstack/react-query ^5.100.10`, `@tanstack/react-table ^8.21.3` | Current. |
| HTTP | `axios ^1.16.1` | Current. |
| Forms | `react-hook-form ^7.76.0`, `zod ^4.4.3`, `@hookform/resolvers ^5.2.2` | Current. |
| UI | `@radix-ui/react-*` (15 primitives), `lucide-react ^1.16.0`, `tailwind-merge ^3.6.0` | Current. |
| Build | `vite ^8.0.13`, `@vitejs/plugin-react ^6.0.2`, `typescript ~5.9.3` | Current. |
| Lint | `eslint ^9.13.0`, `typescript-eslint ^8.11.0`, `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh` | No `eslint-plugin-jsx-a11y`, no `eslint-plugin-security`. |

### Frontend findings

| Severity | Issue | Location | Notes |
|---|---|---|---|
| Medium | **Every** dependency uses a caret range (`^x.y.z`). Combined with the absence of CI / `npm ci` in a build pipeline, `npm install` in two different developers' environments can resolve to different transitives. The committed `package-lock.json` mitigates this — but the build flow needs to actually use it (`npm ci`, not `npm install`). The Dockerfile uses `npm ci` (`frontend/Dockerfile:6`) — good. | `frontend/package.json` (all entries); `frontend/Dockerfile:6` | Risk localised; mention for completeness. |
| Low | No `eslint-plugin-security` configured. With no test suite, the lint pass is the only static-analysis gate the frontend has. | `frontend/eslint.config.js` | Modest gap — `eslint-plugin-security` catches a small set of patterns (timing-safe equals, eval, etc.) most of which don't apply to a CRUD SPA. |
| Low | No `npm audit` or Dependabot. Same root cause as the backend finding above. | `frontend/package.json`; `.github/` absent | Vulnerable transitives are not surfaced. |
| Info | CLAUDE.md describes the frontend as "React 18 + TypeScript … Tailwind 3" (line 41), but `package.json` actually has `react ^19.2.6` and `tailwindcss ^4.3.0`. Documentation drift, not a security issue, but it signals the docs were not refreshed when the major versions bumped. | `CLAUDE.md:41` vs `frontend/package.json:35, 57` | Affects audit accuracy and developer onboarding. |

---

## 3. Recommendations

### Immediate
- Enable Dependabot (`.github/dependabot.yml`) with weekly checks against `pom.xml` and `package.json`.
- Verify `spring-boot-devtools` does not appear in the production runtime classpath (inspect `target/dependency/BOOT-INF/lib/` of a built jar).

### Short-term
- Add `org.owasp:dependency-check-maven` to `pom.xml` and a `npm audit --omit=dev` step to the frontend build.
- Wire both into a GitHub Actions workflow (see `infrastructure.md` for the CI gap).
- Update `CLAUDE.md:41` to reflect React 19 + Tailwind 4.

### Long-term
- Adopt Renovate or a similar bot for grouped upgrade PRs.
- Add SBOM generation (`cyclonedx-maven-plugin`, `@cyclonedx/cyclonedx-npm`).

---

## Conclusion

The currently-pinned versions are **acceptable** at audit time — none of them carry public Critical/High CVEs known as of 2026-05-26 to this auditor. The gap is process: nothing in the repo will *tell you* when a CVE drops against jjwt 0.12.6 or springdoc 2.8.13 or axios 1.16.1. Wire Dependabot + a CI vuln-scan as the immediate fix.
