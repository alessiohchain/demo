---
genre: infrastructure
category: ui-security
analysis-type: static
relevance:
  file-patterns:
    - "**/components/**"
    - "**/views/**"
    - "**/pages/**"
  keywords:
    - "xss"
    - "csp"
    - "innerHTML"
    - "dangerouslySetInnerHTML"
    - "sanitize"
  config-keys:
    - "dompurify"
    - "helmet"
    - "csp"
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# UI-Security Infrastructure Maturity

**Assessment Date:** 2026-05-26
**Status:** Complete

---

<!-- analysis: static -->

## Maturity score — **3 / 5 (Functional)**

The React 19 frontend uses idiomatic JSX (no `dangerouslySetInnerHTML` found), forms are zod-validated, and the access token is held only in memory. Maturity loses ground for the absence of CSP / security headers and the lack of a sanitiser dependency in anticipation of the metadata-driven UI roadmap.

| Control | Present? | Source |
|---|---|---|
| `dangerouslySetInnerHTML` / `innerHTML` usages | None | grep'd `frontend/src/**` |
| Server-error markup stripped before display | Yes — `stripInlineErrorMarkup` | `frontend/src/app/api/client.ts:7, 167` |
| Form validation (zod) | Yes | `frontend/package.json:43`; `react-hook-form` resolver |
| Access token in `localStorage` / `sessionStorage` | No — in-memory only | `frontend/src/app/api/client.ts:9` |
| Refresh token in `localStorage` | No — HTTP-only cookie | `backend/src/main/java/za/co/csnx/demo/web/AuthController.java:75-87` |
| `withCredentials: true` for cookie-bearing requests | Yes | `frontend/src/app/api/client.ts:15` |
| HTML-sanitiser dependency (`dompurify`) | No | `frontend/package.json` |
| CSP header | No | `frontend/nginx.conf.template` |
| `X-Frame-Options` | No | (same) |
| `X-Content-Type-Options` | No | (same) |
| `Referrer-Policy` | No | (same) |
| Subresource Integrity for external assets | None used (Vite-bundled) | — |

---

## Findings

(See security `ui-security.md` for the full write-ups.)

| Severity | Issue | Location | Recommendation |
|---|---|---|---|
| Medium | No CSP / security headers at nginx or Spring layer. | `frontend/nginx.conf.template`; `backend/src/main/java/za/co/csnx/demo/config/SecurityConfig.java` | Add via both layers. |
| Medium | No HTML-sanitiser dependency. Acceptable today (no `innerHTML` paths) but the metadata-driven UI work flagged in `CLAUDE.md:195-203` will need one. | `frontend/package.json` | Add `dompurify` pre-emptively. |
| Low | Tokens in module-scope variable — XSS-readable. | `frontend/src/app/api/client.ts:9, 19` | Acceptable trade-off; the CSP above is the multiplier. |

---

## Maturity verdict — **3**

Safe-by-construction patterns (no innerHTML, zod-validated forms, in-memory tokens) carry this above the implementation-quality baseline, but the missing defence-in-depth headers keep it at 3.
