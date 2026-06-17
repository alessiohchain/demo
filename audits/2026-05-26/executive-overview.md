# Executive Overview — Codebase Audit 2026-05-26

> **Generated:** 2026-05-26 (code-audit 0.4.1 — prompts-only bundle, security + infrastructure genres)
> **Assessment Period:** Snapshot at 2026-05-26 (no git history available in audit environment)
> **Repo:** `C:\software\projects\modules\demo` (demo / CSNX-13935 — Spring Boot + React reference module)

---

## 📊 Executive Summary

**Overall Health Score: 23 / 100** — Critical

**Risk Level:** 🔴 Critical

### At a Glance

| Metric | Value | Status |
|--------|-------|--------|
| Total Lines of Code analysed | ~5 200 | — |
| Total unique findings | 20 (security) + ~13 (infrastructure-specific) | 🔴 High |
| Critical Issues (security) | 2 | 🔴 Immediate attention |
| High Severity Issues (security) | 7 | 🔴 Action needed |
| Average Infrastructure Maturity | 2.85 / 5 | 🔴 Weak |
| Team Collaboration Score | — | ⏭️ Skipped — disabled in this bundle |

### Key Takeaways

1. **The cryptographic & framework primitives are well-chosen** (Spring Boot 3.5, jjwt 0.12.6, bcrypt, JPA + Flyway, React 19 + Vite, Radix UI). The code-level craftsmanship is solid.
2. **Secret-management discipline is the headline failure.** The JWT signing key has a published default in `application.yml`, repeated in `docker-compose.yml`, and not overridden in `application-prod.yml` — token forgery risk if any deployment forgets the env var. Combined with the seeded `WCS/wcs/wcs123!` default user, every clone of this template inherits the same blast radius.
3. **Process maturity is the other binding constraint.** Zero CI/CD, sparse tests, no dependency-vuln scanner. The "modern stack" judgement is undermined by no automation to keep it modern.

### Top 3 Priorities

1. 🔴 **Eliminate the hardcoded JWT secret.** Fix `application.yml:52`, `docker-compose.yml:28`, and add an override line to `application-prod.yml`. Effort: hours. Impact: removes the Critical finding from the security rubric and lifts the security score by ~30 points.
2. 🟡 **Add a baseline GitHub Actions workflow.** `mvnw verify` + `npm ci && lint && build` + `docker build` smoke. Effort: half a day. Lifts three infrastructure dimensions simultaneously.
3. 🟡 **Add a `roles` claim + per-endpoint `@PreAuthorize`.** The access-control dimension currently scores 1 — the lowest in the audit — and applies a 10-point penalty to the infrastructure score.

---

## 🎯 Overall Health Score

**Score: 23 / 100**

| Genre | Weight | Score | Grade | Weighted Contribution |
|-------|-------:|------:|:-----:|---------------------:|
| 🔒 Security | 55% | 15 / 100 | F | 8.3 |
| 🏗️ Infrastructure | 45% | 32 / 100 | D | 14.4 |
| 👥 Team | — | — | — | Skipped — disabled in this bundle |
| ☁️ Hosting | — | — | — | Skipped — disabled in this bundle |
| **TOTAL** | **100%** | | | **~22.7 → 23** |

**Grade Scale:** A (90-100) • B (75-89) • C (55-74) • D (30-54) • F (0-29)

<details>
<summary><b>📖 Scoring Methodology</b> (click to expand)</summary>

Each genre uses a 5-level rubric. Weights are renormalised from the defaults since team and hosting are disabled in this prompts-only bundle:

- Default weights: security 35, infrastructure 30, team 20, hosting 15.
- Active-genre weight: 35 + 30 = 65.
- Renormalised: security 35/65 = 53.8 % ≈ **55 %**; infrastructure 30/65 = 46.2 % ≈ **45 %**.

The security score (15) is computed by the per-1000-LOC rubric in `audit-reviewer.agent.md`:
- Critical/1K LOC ≈ 0.385 — exceeds Level 2's 0.3 ceiling → **Level 1 (score 15)**.
- "Authentication bypass or SQL-injection Critical" → cap at Level 2 (42). The Level-1 score is already below this cap, so the cap does not apply.

The infrastructure score (32) applies the minimum-dimension penalty: base 42 (Level 2) − 10 (penalty for Access control = 1) = **32**.

</details>

---

## 🔒 Security Score Breakdown

**Score: 15 / 100** — Level 1 — Critical

### Scoring Rubric

| Level | Score | Your Status | Criteria (per 1,000 LOC) |
|-------|------:|:-----------:|--------------------------|
| **5 — Excellent** | 95 | ❌ | No Critical, ≤0.1 High, ≤0.5 total findings |
| **4 — Good** | 82 | ❌ | No Critical, ≤0.3 High, ≤1.5 total findings |
| **3 — Fair** | 65 | ❌ | ≤0.1 Critical, ≤0.8 High, ≤3.0 total findings |
| **2 — Poor** | 42 | ❌ | ≤0.3 Critical, ≤2.0 High, ≤6.0 total findings |
| **1 — Critical** | **15** | **✅** | Exceeds Level 2 thresholds |

### Your Metrics

| Metric | Value | Normalized (per 1K LOC) |
|--------|------:|------------------------:|
| Total LOC | ~5 184 | — |
| Critical Findings | 2 | 0.39 |
| High Findings | 7 | 1.35 |
| Medium Findings | 6 | 1.16 |
| Low Findings | 4 | 0.77 |
| Info Findings | 1 | 0.19 |
| **Total Findings** | **20** | **3.85** |

**Special Considerations:**
- ❌ Zero Critical AND zero High findings (bonus +5): does not apply (2 Crit + 7 High).
- ❌ No authentication bypass or SQL injection Critical findings: the JWT-secret finding **is** functionally an auth-bypass class.

<details>
<summary><b>📋 Top Security Findings</b> (click to expand)</summary>

| Severity | Finding | Location | Impact |
|----------|---------|----------|--------|
| Critical | JWT signing secret has hardcoded default | `backend/src/main/resources/application.yml:52` | Token forgery if env var unset |
| Critical | JWT secret shipped literally in compose | `docker-compose.yml:28` | Same — for every compose run |
| High | Seeded `WCS/wcs/wcs123!` user on every startup | `backend/src/main/java/za/co/csnx/demo/service/AppUserSeeder.java:28` | Default credentials in every deployment |
| High | Refresh cookie missing `Secure` + `SameSite` | `backend/src/main/java/za/co/csnx/demo/web/AuthController.java:82-87` | MITM + CSRF risk |
| High | CSRF disabled while cookie auth in use | `backend/src/main/java/za/co/csnx/demo/config/SecurityConfig.java:42` | CSRF against `/api/auth/refresh` |
| High | No login rate-limit / lockout | (absent) | Unlimited credential stuffing |
| High | No per-endpoint RBAC; empty authorities | `JwtAuthFilter.java:71`, `SecurityConfig.java:58` | All authenticated users equal |
| High | Hardcoded Postgres credentials | `docker-compose.yml:5-7, 26-27` | `demo/demo` shared across deployments |
| High | Tenant isolation not enforced | service-layer queries (manual verification needed) | Cross-company reads possible |
| High | `application-prod.yml` doesn't override JWT secret | `backend/src/main/resources/application-prod.yml` | Prod falls through to dev default |

[See `security/vulnerability-report.md` and per-template files in `security/` for the full list.]

</details>

---

## 🏗️ Infrastructure Score Breakdown

**Score: 32 / 100** — Level 2 with penalty — Poor

### Scoring Rubric

| Level | Score | Your Status | Criteria (maturity dimensions 1-5) |
|-------|------:|:-----------:|-------------------------------------|
| **5 — Excellent** | 95 | ❌ | Average ≥4.5, no dimension below 4 |
| **4 — Good** | 82 | ❌ | Average ≥3.8, no dimension below 3 |
| **3 — Fair** | 65 | ❌ | Average ≥2.8, no dimension below 2 |
| **2 — Poor** | **42** | **✅** | Average ≥2.0 |
| **1 — Critical** | 15 | ❌ | Average <2.0 |

### Your Metrics

| Dimension | Score (1-5) | Status | Key Notes |
|-----------|------------:|:------:|-----------|
| Back-end (Spring Boot) | 4.0 | 🟢 | Idiomatic; clean error envelope; multi-stage Docker. |
| Front-end (React 19 / Vite) | 3.0 | 🟡 | Modern stack; zero tests; doc drift. |
| API | 4.0 | 🟢 | OpenAPI auto-gen; no versioning / rate limit. |
| Database | 4.0 | 🟢 | Flyway + Testcontainers; missing-migration signal. |
| Authentication | 3.0 | 🟡 | Sound primitives; weak secret-management. |
| Access control | **1.0** | 🔴 | **No RBAC. Binding-minimum dimension.** |
| Crypto usage | 3.0 | 🟡 | Library choices fine; secret distribution failed. |
| Infrastructure (CI/CD, IaC) | 2.0 | 🔴 | Zero CI/CD; Azure IaC markdown-only. |
| Secure coding | 2.0 | 🔴 | No SAST / scanner. |
| Secure logging | 2.0 | 🔴 | log4j2 chosen well; dev-grade defaults. |
| UI security | 3.0 | 🟡 | No CSP; no sanitiser. |
| Accessibility | 3.0 | 🟡 | Radix baseline good; no a11y lint. |
| Third-party dependencies | 3.0 | 🟡 | Pins current; no Dependabot. |
| **Average** | **2.85** | | |

**Penalty Applied:** −10 points (minimum dimension = 1 → `max(0, (3 − 1) × 5) = 10`).

**Base level:** Level 2 (average ≥ 2.0, but Access control < 2 disqualifies Level 3) → 42 − 10 = **32**.

<details>
<summary><b>📋 Infrastructure Highlights</b> (click to expand)</summary>

**Strengths:**
- Spring Boot 3.5 + Java 21 + JPA + Flyway with `ddl-auto: validate`.
- Multi-stage Docker with non-root backend.
- `CLAUDE.md` as a living convention document.
- React 19 + Vite + TS strict + Radix UI primitives.
- Testcontainers-backed integration tests.

**Areas for Improvement:**
- Add CI/CD (any platform).
- Add `roles` claim + `@PreAuthorize` on write endpoints.
- Add frontend test framework (Vitest).
- Switch compose default profile away from `dev` (so `bind: TRACE` doesn't ship).
- Replace literal secrets in `.yml` / `docker-compose.yml` with `${VAR:?}` requirements.
- Reconcile docs (`CLAUDE.md`, `README.md`) with code reality.

[See `infrastructure/executive-summary.md` and per-dimension files for details.]

</details>

---

## 👥 Team Score Breakdown

**Skipped — disabled in this bundle.** The prompts-only bundle does not include team-genre templates or the team-auditor agent spec. No git history was available in the audit environment to support team metrics.

---

## ☁️ Hosting Score Breakdown

**Skipped — disabled in this bundle.** The prompts-only bundle does not include hosting-genre templates or the hosting-auditor agent spec.

Some hosting-adjacent observations were captured in `infrastructure/infrastructure.md` and `security/infrastructure.md` for the on-disk artefacts that are present:
- `infra/gcp/` Terraform (not deeply audited).
- `.azure/deployment-plan.md` (draft, no Bicep yet).
- `.gcp/deployment-plan.md` (planning doc).
- `docker-compose.yml` for local orchestration.
- `frontend/docker-entrypoint.sh` (multi-cloud nginx entrypoint).

---

## 🔍 Cross-Genre Patterns

<details open>
<summary><b>View Cross-Genre Patterns</b></summary>

### Pattern 1: Secret distribution committed in source

**Genres Affected:** Security, Infrastructure

**Description:** The JWT signing key has a default value in `application.yml:52`, repeated literally in `docker-compose.yml:28`, and not overridden in `application-prod.yml`. Postgres credentials follow the same pattern (`docker-compose.yml:5-7, 26-27`, with defaults at `application.yml:8-9`). The infrastructure-genre and security-genre audits both flag this as their headline finding.

**Impact:** Demo as written cannot be safely deployed to any non-developer environment without manual `.yml` edits — and downstream module clones (per `CLAUDE.md`'s "scaffolding a new module" guidance) will inherit the same defaults.

**Recommendation:** Strip every default value; require the env var; document a `.env.example`. See `security/remediation-plan.md` items I-1, I-2, I-3, I-5.

---

### Pattern 2: Documentation drift undermines audit confidence

**Genres Affected:** Security, Infrastructure

**Description:** `CLAUDE.md:41` says "React 18 + Tailwind 3"; `package.json` has React 19 + Tailwind 4. `README.md:96` documents `POST /api/auth/register` not present in `AuthController.java`. The frontend POSTs `/api/auth/change-password` but no handler appears in the read sample. `V1__init.sql` doesn't contain the active `app_user` schema referenced by the domain entity.

**Impact:** Each drift wastes audit (and developer) time and signals that **other** documented contracts might also be wrong. Future template-clones will replicate stale docs.

**Recommendation:** Generate API docs from OpenAPI; regenerate the typed frontend client from `/v3/api-docs`; refresh `CLAUDE.md` on every dependency major bump; add a CI step that runs `flyway:validate` against a fresh container.

---

### Pattern 3: Modern primitives, missing operational gates

**Genres Affected:** Security, Infrastructure

**Description:** The codebase has good *primitives* in every dimension — bcrypt, jjwt 0.12.6, Spring Security 6, Testcontainers, multi-stage Docker, TS strict, Radix UI. What it doesn't have is **automation that keeps those primitives healthy** — no CI, no Dependabot, no SAST, no a11y test, no rate limiter, no JWT blacklist, no refresh-token rotation, no log-shipping config.

**Impact:** The "modern stack" claim degrades silently. Today it's all current; without automation, the lag will compound.

**Recommendation:** Bootstrap one CI workflow that wires the gates: `mvnw verify`, `npm ci && lint && build`, `docker build`, `dependency-check`, `npm audit`, Dependabot. Each gate is small; the cumulative impact lifts three or four infrastructure dimensions simultaneously.

</details>

---

## ✅ Priority Action Plan

### 🔴 Immediate (0-7 days) — Critical

- [ ] **Strip hardcoded JWT secret from `application.yml:52`** — Genre: Security
  *Impact:* High | *Effort:* XS
  *Details:* Remove the `:dGhpcy1pcy1hLWRldi1zZWNyZXQtcmVwbGFjZS1pbi1wcm9kdWN0aW9uLW9rPw==` default. Let startup fail-fast if `APP_SECURITY_JWT_SECRET` is missing.

- [ ] **Replace literal JWT secret in `docker-compose.yml:28`** — Genre: Security
  *Impact:* High | *Effort:* XS
  *Details:* Use `${APP_SECURITY_JWT_SECRET:?set in .env}`; commit a `.env.example`.

- [ ] **Add `app.security.jwt.secret: ${APP_SECURITY_JWT_SECRET}` to `application-prod.yml`** — Genre: Security
  *Impact:* High | *Effort:* XS
  *Details:* Prevents prod-profile fall-through to the dev default.

- [ ] **Replace literal Postgres credentials in `docker-compose.yml`** — Genre: Security / Infra
  *Impact:* High | *Effort:* XS
  *Details:* `${POSTGRES_PASSWORD:?}`; remove defaults at `application.yml:8-9`.

- [ ] **Delete `System.out.println("fgfgfggf");` at `DemoBackendApplication.java:16`** — Genre: Security / Infra
  *Impact:* Low | *Effort:* XS

- [ ] **Gate `AppUserSeeder` behind `@Profile("dev")`** — Genre: Security
  *Impact:* High | *Effort:* XS

- [ ] **Set `Secure` + `SameSite=Strict` on refresh cookie (`AuthController.java:82-87`)** — Genre: Security
  *Impact:* High | *Effort:* S
  *Details:* Use Spring's `ResponseCookie`.

### 🟡 Short-term (1-4 weeks) — High

- [ ] **Bootstrap a GitHub Actions workflow** — Genre: Infrastructure
  *Impact:* High | *Effort:* M
  *Details:* `mvnw verify` (Testcontainers OK in Actions), `npm ci && npm run lint && npm run build`, `docker build` smoke. Add Dependabot config.

- [ ] **Add `roles` claim + `@EnableMethodSecurity` + per-endpoint `@PreAuthorize`** — Genre: Security / Infra
  *Impact:* High | *Effort:* M
  *Details:* Lifts the access-control dimension from 1 → 3 and removes the infrastructure-score penalty.

- [ ] **Add rate-limiter on `/api/auth/login`** — Genre: Security
  *Impact:* High | *Effort:* M
  *Details:* `bucket4j-spring-boot-starter` with per-username + per-IP buckets.

- [ ] **Audit every JPA query for company-scope filtering** — Genre: Security
  *Impact:* High | *Effort:* L
  *Details:* Cross-tenant read risk; see `security/access-control.md` finding 2.2.

- [ ] **Switch compose default profile away from `dev`** — Genre: Security / Infra
  *Impact:* Medium | *Effort:* XS
  *Details:* `docker-compose.yml:24`. Stops `bind: TRACE` from logging PII.

- [ ] **Reconcile docs with code** — Genre: Infra
  *Impact:* Medium | *Effort:* M
  *Details:* `CLAUDE.md` React/Tailwind versions; `README.md` API table; verify missing migrations / handlers.

### 🟠 Medium-term (1-3 months) — Moderate

- [ ] **Add Vitest + a baseline frontend test suite** — Genre: Infra
- [ ] **Add CSP / security headers in nginx + Spring** — Genre: Security
- [ ] **Implement refresh-token rotation + JTI blacklist** — Genre: Security
- [ ] **Generate the Azure Bicep IaC from the deployment-plan markdown** — Genre: Infra
- [ ] **Tighten DTO validation (`@Pattern` on companyCode, `@Size` on credentials)** — Genre: Security
- [ ] **Migrate log4j2 PatternLayout → JSON layout; add `MDC` correlation IDs** — Genre: Infra

### 🟢 Long-term (3-6 months) — Low

- [ ] **HS256 → RS256 with `kid` rotation, backed by Key Vault / Secret Manager** — Genre: Security
- [ ] **MFA (TOTP) + forgot-password flow** — Genre: Security
- [ ] **SBOM generation + signed releases** — Genre: Security / Infra
- [ ] **Postgres row-level security on tenant-scoped tables** — Genre: Security / Infra

---

## 🎲 Risk Assessment

**Overall Risk Level:** 🔴 Critical

### Risk Summary

The demo, as it currently stands, is **safe for development use only**. Two compounding factors make any non-dev deployment risky: (1) the JWT signing key has a published default that can sign tokens for arbitrary subjects, and (2) the documented "scaffold a new module by cloning this template" workflow (per `README.md:103-114`) propagates that default into every downstream module unless cloners explicitly know to fix it. The technical foundations are sound — the deployment posture is not.

### Key Risk Factors

1. **JWT token forgery via published signing key** — Critical
   *Likelihood:* High (any deployment without `APP_SECURITY_JWT_SECRET` env) | *Impact:* High (full auth bypass)
   *Mitigation:* Phase-I Immediate items (strip defaults, fail-fast on missing var).

2. **Default credentials baked into every clone** — High
   *Likelihood:* High (every clone replicates the file) | *Impact:* High (`WCS/wcs/wcs123!` + `demo/demo`)
   *Mitigation:* `@Profile("dev")` on the seeder; `${VAR:?}` patterns in compose.

3. **Process maturity ≠ technical maturity** — Medium
   *Likelihood:* Certain (no CI exists) | *Impact:* Medium (drift compounds over time)
   *Mitigation:* One GitHub Actions workflow buys most of the dimensions back.

### Risk Trend

First audit of this repository — no prior baseline. Establish 2026-05-26 as the baseline; re-audit after the Phase-I remediations land.

---

## 📈 Audit Coverage Report

<details>
<summary><b>View Detailed Coverage</b></summary>

### Genres Assessed

| Genre | Status | Templates Filled | Templates Skipped | Coverage |
|-------|--------|-----------------:|------------------:|----------|
| 🔒 Security | ✅ Run | 14 | 3 | 82 % |
| 🏗️ Infrastructure | ✅ Run | 14 | 3 | 82 % |
| 👥 Team | ⏭️ Skipped | — | — | Disabled in this bundle |
| ☁️ Hosting | ⏭️ Skipped | — | — | Disabled in this bundle |

### Templates Skipped

| Template | Genre | Reason |
|----------|-------|--------|
| `security/ai.md` | Security | No AI / LLM / ML code or dependencies |
| `security/mobile.md` | Security | No mobile code (no React Native / Capacitor / iOS / Android) |
| `security/voice.md` | Security | No voice / IVR / telephony / Twilio code |
| `infrastructure/ai.md` | Infrastructure | Same as above |
| `infrastructure/mobile.md` | Infrastructure | Same as above |
| `infrastructure/voice.md` | Infrastructure | Same as above |

### Assessment Scope

**Time Period:** Snapshot at 2026-05-26

**Codebase Scope:**
- Lines of code analysed: ~5 200
- Files analysed (read in full): ~45 (backend Java configs / security / web / common, frontend `app/`, `lib/`, `shell/Login.tsx`, docker / nginx, `pom.xml`, `package.json`)
- Files inventoried via `Glob` / `ls`: ~145 source files in backend + frontend (excluding `node_modules`, `dist`, `target`)
- Directories excluded: `node_modules`, `dist`, `build`, `.git`, `target`, `__pycache__`

**IaC Resources Analysed:** N/A (hosting genre disabled). GCP Terraform under `infra/gcp/` was noted but not deeply read.

</details>

---

## 📎 Appendix

<details>
<summary><b>Finding Counts by Template</b></summary>

### Security Findings

| Template | Critical | High | Medium | Low | Info | Total |
|----------|---------:|-----:|-------:|----:|-----:|------:|
| `authentication.md` | 2 | 3 | 2 | 0 | 0 | 7 |
| `crypto-usage.md` | 1 | 0 | 1 | 1 | 0 | 3 |
| `api.md` | 0 | 2 | 3 | 2 | 0 | 7 |
| `access-control.md` | 0 | 2 | 1 | 0 | 0 | 3 |
| `back-end.md` | 0 | 0 | 3 | 2 | 0 | 5 |
| `database.md` | 0 | 1 | 2 | 1 | 1 | 5 |
| `infrastructure.md` | 1 | 1 | 3 | 2 | 0 | 7 |
| `secure-logging.md` | 0 | 0 | 3 | 2 | 2 | 7 |
| `third-party-dependencies.md` | 0 | 0 | 2 | 3 | 1 | 6 |
| `ui-security.md` | 0 | 0 | 2 | 2 | 0 | 4 |
| `audit-checklist.md` | (roll-up; no new findings) | — | — | — | — | — |
| `vulnerability-report.md` | (roll-up: 20 unique findings) | — | — | — | — | — |
| `remediation-plan.md` | (action plan) | — | — | — | — | — |
| `executive-summary.md` | (roll-up) | — | — | — | — | — |

Note: the same root cause (hardcoded JWT secret) appears in `authentication.md`, `crypto-usage.md`, and `infrastructure.md` as the same finding viewed from three angles. The `vulnerability-report.md` deduplicates these to **20 unique findings**.

### Infrastructure Findings (maturity scores)

| Template | Maturity Score | Key Strengths | Key Gaps |
|----------|---------------:|---------------|----------|
| `back-end.md` | 4 | Spring Boot 3.5; clean error envelope; multi-stage Docker | Test breadth; no CI |
| `front-end.md` | 3 | Modern React 19 / Vite; shadcn convention | Zero tests; doc drift |
| `api.md` | 4 | OpenAPI auto-gen; RFC 7807 errors | No versioning; no rate limit |
| `database.md` | 4 | Flyway + validate + Testcontainers | Missing-migration signal |
| `authentication.md` | 3 | Sound primitives | Secret-management; no rate-limit; no rotation |
| `access-control.md` | **1** | (none) | No RBAC; tenant scoping unenforced |
| `crypto-usage.md` | 3 | bcrypt; jjwt 0.12.6 | Hardcoded secret default |
| `infrastructure.md` | 2 | Container craft | Zero CI/CD; Azure IaC markdown only |
| `secure-coding.md` | 2 | Clean code by inspection | No SAST / scanner / pre-commit |
| `secure-logging.md` | 2 | log4j2 chosen well | Dev defaults bleed into compose |
| `ui-security.md` | 3 | No innerHTML; zod-validated forms | No CSP |
| `accessibility.md` | 3 | Radix baseline | No a11y lint / test |
| `third-party-dependencies.md` | 3 | Pins current | No Dependabot / scanner |

### Team Findings

⏭️ Skipped — disabled in this bundle.

### Hosting Findings

⏭️ Skipped — disabled in this bundle.

</details>

---

**Report prepared by code-audit 0.4.1 (prompts-only bundle)**
**Bundle location:** `D:\software\London_Dev_Track_Resources\a\code-audit-prompts\code-audit-0.4.1-prompts`
*For detailed findings, see individual genre reports in `security/` and `infrastructure/`.*
