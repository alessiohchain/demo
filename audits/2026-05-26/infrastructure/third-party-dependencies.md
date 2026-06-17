---
genre: infrastructure
category: third-party-dependencies
analysis-type: static
relevance:
  file-patterns:
    - "package.json"
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
  config-keys: []
  always-include: true
severity-scale: "Critical|High|Medium|Low|Info"
---

# Third-Party Dependency Maturity

**Assessment Date:** 2026-05-26
**Status:** Complete

---

<!-- analysis: static -->

## Maturity score — **3 / 5 (Functional)**

Dependencies are current and well-pinned. Spring Boot 3.5.14 brings vetted transitives; jjwt 0.12.6 and springdoc 2.8.13 are current; the frontend tracks recent majors. The gap: **no automated mechanism notifies you when a CVE drops** — no Dependabot, no `dependency-check`, no `npm audit` step (and no CI to run them in).

See `security/third-party-dependencies.md` for the per-dependency inventory and findings.

---

## Process gaps that drag the dimension down

| Item | State | Recommendation |
|---|---|---|
| Dependabot / Renovate | Not configured | Add `.github/dependabot.yml` (weekly cadence). |
| OWASP Dependency-Check | Not configured | `org.owasp:dependency-check-maven` plugin. |
| `npm audit` step | Not configured (no CI) | Add to a CI workflow. |
| SBOM generation | Not configured | `cyclonedx-maven-plugin` + `@cyclonedx/cyclonedx-npm`. |
| Lockfile hygiene | `package-lock.json` present; `mvnw` resolves transitively (no Maven lock file — by design) | `npm ci` is used in the frontend Dockerfile ✓ |
| License compliance scan | Not configured | Optional — `license-maven-plugin` / `license-checker`. |

---

## Risk snapshot

- **Backend:** Boot 3.5.14 is on a maintained line (security patches via 3.5.x updates). Non-BOM pins (`jjwt 0.12.6`, `mapstruct 1.6.3`, `springdoc 2.8.13`) are current at audit time but will drift.
- **Frontend:** Major versions are current (React 19, Vite 8, TanStack Query 5, Zod 4). Caret ranges on every entry mean local-install reproducibility depends on the committed lockfile being used (`npm ci`, not `npm install`).
- **Documentation drift** (`CLAUDE.md` lists React 18 / Tailwind 3, actual `package.json` is React 19 / Tailwind 4) suggests the docs were not refreshed on the last major bump.

---

## Maturity verdict — **3**

Current pinned versions are healthy; the process to keep them healthy is missing. Wire Dependabot + a CI vuln-scan step and this dimension jumps to 4.
