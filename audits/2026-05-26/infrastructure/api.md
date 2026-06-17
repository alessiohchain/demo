---
genre: infrastructure
category: api
analysis-type: static
relevance:
  file-patterns:
    - "**/api/**"
    - "**/routes/**"
    - "**/controllers/**"
    - "**/graphql/**"
  keywords:
    - "api"
    - "endpoint"
    - "rest"
    - "graphql"
    - "swagger"
    - "openapi"
  config-keys:
    - "express"
    - "fastify"
    - "@nestjs/core"
    - "flask"
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# API Infrastructure Maturity

**Assessment Date:** 2026-05-26
**Status:** Complete

---

<!-- analysis: static -->

## Maturity score — **4 / 5 (Modern)**

REST/JSON API with OpenAPI auto-generation, consistent error envelope, JWT bearer auth, sensible URL design. Loses a level for missing rate-limiting, no API versioning strategy, and no generated client.

| Item | State | Source |
|---|---|---|
| API style | REST/JSON over Spring MVC | `backend/src/main/java/za/co/csnx/demo/web/*Controller.java` |
| OpenAPI generator | springdoc-openapi 2.8.13 | `backend/pom.xml:91` |
| OpenAPI docs path | `/v3/api-docs`, `/swagger-ui.html` | `backend/src/main/resources/application.yml:38-43` |
| Security scheme | Bearer JWT declared in OpenAPI | `backend/src/main/java/za/co/csnx/demo/config/OpenApiConfig.java:25-29` |
| Error format | RFC 7807 `ProblemDetail` with `kind` discriminator | `backend/src/main/java/za/co/csnx/demo/common/GlobalExceptionHandler.java:36-147` |
| Versioning | None — no `/v1/`, no `Accept: application/vnd.demo.v1+json` | `web/*Controller.java` — URLs are unversioned |
| Rate limiting | None | (no filter in `SecurityConfig`) |
| CORS | Configured allow-list | `SecurityConfig.java:63-72` |
| Content negotiation | Default (JSON only) | Spring defaults |
| Pagination | Not exercised in audit-sample endpoints; `JpaSpecificationExecutor` is available | — |

---

## Endpoints

| Method | Path | Purpose | Auth |
|---|---|---|---|
| POST | `/api/auth/login` | Username + password → JWT + refresh cookie | none |
| POST | `/api/auth/refresh` | Refresh-cookie → JWT | cookie |
| POST | `/api/auth/logout` | Clear refresh cookie | none |
| POST | `/api/auth/change-password` | (referenced by frontend; handler not in `AuthController.java`) | none |
| GET | `/api/lookup/init` | Pre-login Company VVD; post-login full bundle | partial |
| GET | `/api/lookup/**` (other) | VVD data | JWT |
| GET | `/api/metadata/**` | Screen metadata | JWT |
| POST | `/api/process/**` | Workflow dispatch | JWT |
| GET | `/actuator/health`, `/info` | Liveness | none |
| GET | `/v3/api-docs/**`, `/swagger-ui.html` | OpenAPI | none |

---

## Findings

| Severity | Issue | Location | Recommendation |
|---|---|---|---|
| Medium | No API versioning. The README's "Future direction" hints at significant evolution (metadata-driven UI) — non-versioned URLs make the inevitable breaking changes painful. | `backend/src/main/java/za/co/csnx/demo/web/*Controller.java` (all use `/api/...`) | Move to `/api/v1/...` before the first external consumer. |
| Medium | No rate limiting on any endpoint. | (no filter in `backend/src/main/java/za/co/csnx/demo/config/SecurityConfig.java`) | Add `bucket4j-spring-boot-starter`. See security audits. |
| Medium | Implementation/docs drift — `/api/auth/register` documented in `README.md:96` but no handler; `/api/auth/change-password` called from frontend (`AuthProvider.tsx:254`) but no handler in `AuthController.java`. | `README.md:96`; `backend/src/main/java/za/co/csnx/demo/web/AuthController.java` | Verify the missing handlers; regenerate docs from OpenAPI to stop drift. |
| Low | No generated client — the frontend hand-types DTOs that mirror the backend (`CLAUDE.md:108` mentions this is "the long-term plan"). | `frontend/src/app/api/client.ts`; `frontend/src/app/auth/AuthProvider.tsx` | Wire `openapi-typescript` or `orval` so the frontend types are generated from `/v3/api-docs`. |
| Low | All authenticated endpoints share a single binary check (`anyRequest().authenticated()`). No per-endpoint authorities. | `backend/src/main/java/za/co/csnx/demo/config/SecurityConfig.java:58` | See `access-control.md`. |
| Info | OpenAPI Bearer security scheme is correctly declared, so Swagger UI works for live tests. | `backend/src/main/java/za/co/csnx/demo/config/OpenApiConfig.java:23-29` | — |

---

## Maturity verdict — **4**

The API surface is small, idiomatic, and OpenAPI-described. The lack of versioning and rate-limiting are the realistic Level-5 gaps. Generated client + versioning would push this to 5.
