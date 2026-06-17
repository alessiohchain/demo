---
genre: infrastructure
category: executive-summary
analysis-type: static
relevance:
  file-patterns: []
  keywords: []
  config-keys: []
  always-include: true
severity-scale: "Critical|High|Medium|Low|Info"
---

# Infrastructure Executive Summary

**Assessment Date:** 2026-05-26
**Prepared by:** code-audit (infrastructure-auditor agent)
**Project:** demo (Spring Boot + React reference module, CSNX-13935)

---

<!-- analysis: static -->

## Bottom line

The demo's **technical choices are modern and idiomatic** — Spring Boot 3.5, Java 21, JPA + Flyway with `ddl-auto: validate`, React 19 + Vite + TS strict, Radix UI primitives, Testcontainers, multi-stage Docker, log4j2-only logging. The **maturity gap is process and operations**: zero CI/CD, sparse tests, no automated dependency / vulnerability scanning, dev-grade logging config that bleeds into the default compose profile, and an access-control dimension that is functionally empty.

---

## Per-dimension scores

| Dimension | Score (1-5) | Status | Key notes |
|---|---:|---|---|
| Architecture / back-end | **4** | 🟢 | Idiomatic Spring Boot 3.5; clean error envelope; multi-stage Docker; non-root user. |
| Architecture / front-end | **3** | 🟡 | Modern stack but zero tests; doc drift on React/Tailwind majors. |
| API | **4** | 🟢 | OpenAPI auto-gen; RFC 7807 errors; clean controller layer. No versioning / rate limit. |
| Database | **4** | 🟢 | Flyway + `validate` + Testcontainers; missing-migration signal in this snapshot. |
| Authentication | **3** | 🟡 | Sound primitives; weak secret-management + missing operational controls. |
| Access control | **1** | 🔴 | No RBAC. Empty authorities. Tenant scoping not enforced. **Binding-minimum dimension.** |
| Crypto usage | **3** | 🟡 | Library + algorithm choices fine; key-distribution failed. |
| Infrastructure (containers, IaC, CI/CD) | **2** | 🔴 | Container craft is good; **zero CI/CD**; Azure IaC is markdown only. |
| Secure coding | **2** | 🔴 | No SAST / lint / dependency scanner enforced. |
| Secure logging | **2** | 🔴 | log4j2 chosen well; dev-grade config bleeds into compose default. |
| UI security | **3** | 🟡 | No `innerHTML`, zod-validated forms; no CSP. |
| Accessibility | **3** | 🟡 | Radix baseline good; no a11y lint / test. |
| Third-party dependencies | **3** | 🟡 | Pins are current; no Dependabot / scanner. |

**Average:** ~2.85 / 5
**Minimum dimension:** 1 (Access control)
**Maximum dimension:** 4 (Back-end, API, Database)

---

## Audit-reviewer scoring

Per the rubric in `agents/audit-reviewer.agent.md`:

- Average ≥ 2.8 ✓ (just) and no dimension below 2? **Fails** (Access control = 1). Falls through to Level 2.
- Level 2 requires average ≥ 2.0 (✓) → **Base infrastructure score: 42**.
- Penalty for weak dimension: `max(0, (3 - 1) * 5) = 10`.
- **Final infrastructure score: 42 - 10 = 32**.

The minimum-dimension (Access control = 1) is the binding constraint. Lifting that single dimension to 3 (functional RBAC) — which is a documented Phase-II remediation in `security/remediation-plan.md` — would lift the overall score by roughly 25 points.

---

## Top strengths (preserve these)

1. **Spring Boot 3.5 + Java 21 + JPA + Flyway** — modern, well-discoverable, and `CLAUDE.md` documents every convention for downstream module clones.
2. **Multi-stage Docker images** with non-root backend, layer-split fat-jar for warm builds, and a runtime-templated nginx config that works across local-compose / Azure / GCP without rebuilding.
3. **`open-in-view: false` and `ddl-auto: validate`** — explicit-control patterns that avoid the common Spring Boot footguns.
4. **Type-discriminated JWT** (`typ: access` vs `typ: refresh`) — closes a known jjwt misuse class.
5. **CLAUDE.md** as a living convention document — readable, opinionated, and accurate where it matches code.

---

## Top weaknesses (address these)

1. **Zero CI/CD.** Tests, lint, dependency scan, and IaC validation all live on developer machines.
2. **Access-control dimension is empty.** No RBAC, no per-endpoint authorities, no enforced tenant scoping. Drags the overall infrastructure score down by ~25 points alone.
3. **Secret-management discipline.** Dev defaults committed in `application.yml`, `docker-compose.yml`, prod profile fails to override.
4. **Test coverage gap.** Frontend has zero tests; backend has 4 test files for ~55 source files.
5. **Documentation drift.** `CLAUDE.md:41` lists React 18 / Tailwind 3; reality is React 19 / Tailwind 4. README mentions `/api/auth/register` not present in code. `V1__init.sql` does not contain the active schema (`app_user` and friends).

---

## Recommendation priority (infra-flavoured roll-up — see `remediation-plan.md` for full)

1. **Add a baseline GitHub Actions workflow** — `mvnw verify`, `npm ci && npm run lint && npm run build`, `docker build` smoke, `terraform validate` on `infra/gcp/`. Effort: M. Impact: lifts CI/CD dimension 1 → 3, lifts secure-coding 2 → 3, infrastructure 2 → 3.
2. **Add a `roles` claim + `@EnableMethodSecurity` + `@PreAuthorize` on write endpoints.** Effort: M. Impact: lifts access-control 1 → 3, removes the binding minimum-dimension penalty.
3. **Add Vitest + a handful of frontend tests** + extend backend slice tests. Effort: M. Impact: lifts front-end 3 → 4.
4. **Switch compose `SPRING_PROFILES_ACTIVE` default to `prod`; move `bind: TRACE` to a `verbose` profile.** Effort: XS. Impact: lifts secure-logging 2 → 3.
5. **Replace literal secrets in compose / yml with `${VAR:?}` requirements.** Effort: S. Impact: lifts crypto-usage 3 → 4, authentication 3 → 4, infrastructure 2 → 3.

Applying items 1–5 in order would lift the overall infrastructure score from 32 → roughly 70 (Level 3 — Fair).

---

## Skipped templates

The following templates were applicable to the bundle but not the project; recorded as skipped in `audit-metadata.json`:

| Template | Reason |
|---|---|
| `infrastructure/mobile.md` | No mobile code, no React Native / Capacitor / iOS / Android in `frontend/package.json`. |
| `infrastructure/voice.md` | No voice / IVR / telephony / Twilio code. |
| `infrastructure/ai.md` | No LLM / AI / ML code or dependencies. |
