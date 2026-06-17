---
genre: infrastructure
category: infrastructure
analysis-type: static
relevance:
  file-patterns:
    - "**/docker*"
    - "**/k8s/**"
    - "**/terraform/**"
    - "docker-compose*"
  keywords:
    - "docker"
    - "kubernetes"
    - "container"
    - "helm"
    - "terraform"
    - "nginx"
  config-keys: []
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# Infrastructure Maturity (containerisation, IaC, CI/CD)

**Assessment Date:** 2026-05-26
**Status:** Complete

---

<!-- analysis: static -->

## Maturity score — **2 / 5 (Outdated / Significant gaps)**

Container images and compose orchestration are well-built. The two dimensions that hold this back:
- **No CI/CD pipeline of any kind.** No `.github/workflows/`, no `.gitlab-ci.yml`, no `Jenkinsfile`. The only build automation is local `mvnw` / `npm` invocations.
- **IaC is incomplete.** `infra/gcp/` has Terraform files; the Azure plan (`.azure/deployment-plan.md`) is a draft Markdown document, not Bicep/Terraform code.

| Dimension | Score | Notes |
|---|---|---|
| Container images | 4 | Multi-stage, non-root backend, sane image tags |
| Local orchestration (compose) | 3 | Works; secret defaults & port exposure pull score down |
| CI/CD pipeline | 1 | **None** |
| IaC for cloud | 2 | Partial GCP Terraform; Azure plan only |
| Observability infra | 2 | Actuator endpoints + log4j2 stdout; no central shipping |
| Secrets management | 1 | All in `.yml` files |

---

## 1. Containers

### Backend `backend/Dockerfile`

```
FROM eclipse-temurin:21-jdk-alpine AS build   # multi-stage
RUN ./mvnw dependency:go-offline               # layer-cached deps
COPY src src
RUN ./mvnw package && (cd target/dependency; jar -xf ../*.jar)

FROM eclipse-temurin:21-jre-alpine             # smaller runtime
RUN addgroup -S app && adduser -S app -G app   # non-root
WORKDIR /app
COPY --from=build ... /app/lib                  # layer-split fat-jar
USER app                                        # drops to non-root
EXPOSE 8080
ENTRYPOINT [...exec java...]
```

**Strengths:** multi-stage; non-root user; OCI layer split for warm rebuilds; `eclipse-temurin` is a current well-maintained base.

**Gaps:** no `HEALTHCHECK` in Dockerfile (the compose-level healthcheck covers it but a Dockerfile-level one helps in non-compose deployments).

### Frontend `frontend/Dockerfile`

```
FROM node:22-alpine AS build
RUN npm ci   # reproducible
RUN npm run build

FROM nginx:1.27-alpine
RUN apk add --no-cache gettext   # envsubst
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf.template ...
COPY docker-entrypoint.sh /usr/local/bin/...
HEALTHCHECK ...
ENTRYPOINT ["/usr/local/bin/demo-entrypoint.sh"]
```

**Strengths:** multi-stage; entrypoint script renders nginx config at runtime so the same image runs locally, on Azure Container Apps, and on Cloud Run; `HEALTHCHECK` defined.

**Gaps:** no explicit `USER` directive — nginx runs as root (the upstream `nginx:1.27-alpine` image has a `nginx` user but doesn't drop to it by default).

### Compose `docker-compose.yml`

| Service | Image | Ports | Healthcheck | Volumes |
|---|---|---|---|---|
| postgres | postgres:16-alpine | 5432:5432 (host) | `pg_isready` | `postgres-data` volume |
| backend | (built from `./backend`) | 8080:8080 | `wget actuator/health` | — |
| frontend | (built from `./frontend`) | 8081:80 | (in Dockerfile) | — |

**Strengths:** depends_on with `condition: service_healthy`; named volume for Postgres data; clean three-tier.

**Gaps:** hardcoded `demo/demo` credentials; hardcoded JWT secret; Postgres port exposed; no `security_opt` / `read_only` / `cap_drop` hardening.

---

## 2. IaC

### GCP

`infra/gcp/` directory exists with a `README.md`. Terraform files (`*.tf`) were not deeply read in this audit. `.gcp/deployment-plan.md` is a planning document.

### Azure

`.azure/deployment-plan.md` — Markdown plan for Container Apps + Postgres Flexible Server, no Bicep yet. Listed as draft.

| Severity | Issue | Location | Recommendation |
|---|---|---|---|
| High | No Azure IaC (Bicep / Terraform) — only a Markdown plan. The "deploy to Azure" path would currently be manual. | `.azure/deployment-plan.md` | Generate Bicep using the `azure-prepare` skill referenced in `CLAUDE.md:181-194`. |
| Medium | GCP Terraform is present (`infra/gcp/`) but neither this audit nor any CI verifies it (`terraform validate` / `terraform plan`). | `infra/gcp/` | Add a CI step that runs `terraform fmt -check && terraform validate`. |

---

## 3. CI/CD

**Nothing.** No `.github/workflows/`, no `.gitlab-ci.yml`, no `Jenkinsfile`, no `azure-pipelines.yml`. Backend tests, frontend lint, and Docker builds are entirely on-developer-machine.

| Severity | Issue | Location | Recommendation |
|---|---|---|---|
| High | No CI pipeline. | (entire repo) | Bootstrap a GitHub Actions workflow with: backend `mvnw verify` (Testcontainers-compatible), frontend `npm ci && npm run lint && npm run build`, `docker build` smoke test, optionally `terraform validate` on the GCP module. |
| High | No dependency-vulnerability scanning. | (same as above) | Same workflow: `dependency-check-maven`, `npm audit`, Dependabot. |

---

## 4. Observability

| Item | State |
|---|---|
| Liveness | `/actuator/health` (Spring Boot Actuator) |
| Readiness | (`/actuator/health/readiness` available but not separately exposed) |
| Metrics | `/actuator/metrics` exposed (`application.yml:33`) but no Prometheus / OTLP scraping configured |
| Tracing | None |
| Log shipping | None — stdout only |
| Errors / Sentry | None |

| Severity | Issue | Location | Recommendation |
|---|---|---|---|
| Medium | No Prometheus / Micrometer / OTLP export configured. | `backend/pom.xml` (no `micrometer-registry-*` deps) | Add `micrometer-registry-prometheus` and expose `/actuator/prometheus`. |
| Medium | No central log aggregation, no log format pinned to JSON. | `backend/src/main/resources/log4j2-spring.xml` | See `secure-logging.md`. |
| Low | No `sentry-spring-boot-starter` or similar. | `backend/pom.xml` | Add when first non-local environment lands. |

---

## Maturity verdict — **2**

Container craftsmanship is good. The two zero-scoring gaps (CI/CD = 0, full Azure IaC = 0) drag the dimension hard. With a baseline GitHub Actions workflow added, this lifts to 3 immediately.
