---
genre: security
category: ui-security
analysis-type: static
relevance:
  file-patterns:
    - "**/components/**"
    - "**/views/**"
    - "**/pages/**"
    - "**/templates/**"
    - "**/public/**"
  keywords:
    - "xss"
    - "csp"
    - "innerHTML"
    - "dangerouslySetInnerHTML"
    - "sanitize"
    - "escape"
    - "script"
    - "iframe"
  config-keys:
    - "dompurify"
    - "xss"
    - "helmet"
    - "csp"
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# UI Security Assessment

**Assessment Date:** 2026-05-26
**Auditor:** code-audit (security-auditor agent)
**Status:** Complete

---

<!-- analysis: static -->

## Executive Summary

**Overall Rating:** [ ] Excellent [x] Good [ ] Fair [ ] Poor [ ] Critical

**Findings:** Critical: 0 | High: 0 | Medium: 2 | Low: 2

The React 19 frontend uses idiomatic JSX (no `dangerouslySetInnerHTML` found in `src/**`), forms are validated with `react-hook-form + zod`, and the access token is held in-memory rather than localStorage (a deliberate choice — see `AuthProvider.tsx:159`). The risks are the **absence of a Content-Security-Policy**, no XSS-prevention library in the dependency tree, and access-token storage that does not survive a tab reload.

---

## 1. Architecture

- Build: Vite 8 + React 19 + TypeScript strict.
- Components: `frontend/src/components/ui/` (shadcn primitives) and `frontend/src/shell/` (auth-bearing routes).
- State: TanStack Query for server state, `react-hook-form` + `zod` for forms.
- Tokens: access token in module-scope variable (`frontend/src/app/api/client.ts:9`); refresh token in HTTP-only cookie.
- API client: axios with `withCredentials: true`.

---

## 2. Findings

### 2.1 No Content-Security-Policy

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Medium | The nginx config that fronts the production build (`frontend/nginx.conf.template`) does not emit a `Content-Security-Policy` header, nor an `X-Frame-Options`, `X-Content-Type-Options`, or `Referrer-Policy`. The backend's `SecurityConfig` also does not configure Spring Security's `headers()` block. | `frontend/nginx.conf.template:1-26`; `backend/src/main/java/za/co/csnx/demo/config/SecurityConfig.java:36-61` | XSS impact would be unbounded — any injected script can exfiltrate the in-memory access token (cookie is HTTP-only, but the JS-accessible token is the high-value target). |

### 2.2 In-memory access token survives the SPA lifetime

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Medium | The access token is stored in a closed-over module-scope variable `let accessToken: string | null` (`frontend/src/app/api/client.ts:9`). Any script running in the same global context (XSS, malicious browser extension, or a third-party script loaded by accident) can read it via the closed-over `setAccessToken` if exported (which it is — line 19) or simply by reading the next outbound `Authorization` header. | `frontend/src/app/api/client.ts:9, 19` | Better than `localStorage` (no persistence across tabs/sessions), but still XSS-readable via interception. Acceptable in this architecture; the CSP gap above is the multiplier. |

### 2.3 No use of `dangerouslySetInnerHTML` / `innerHTML`

| Severity | Status | Notes |
|---|---|---|
| Pass | grep'd `dangerouslySetInnerHTML` and `innerHTML` across `frontend/src/**` — no usages. The `stripInlineErrorMarkup` helper at `frontend/src/app/api/client.ts:7` suggests the codebase actively strips inline markup before display rather than rendering raw HTML. |

### 2.4 Error messages from server are rendered as text

| Severity | Status | Notes |
|---|---|---|
| Pass | `client.ts:167` renders server `detail` via `toast.error(stripInlineErrorMarkup(...))` — text-only. No HTML escaping is necessary because sonner toasts render text content as text. |

### 2.5 User-controlled form values

| Severity | Status | Notes |
|---|---|---|
| Pass | Login form uses controlled inputs (`Login.tsx:189-214`); `autoComplete="username"` / `"current-password"` set correctly. No `v-html` / `outerHTML` writes. |

### 2.6 No `dompurify` / `xss` / `helmet`-equivalent dependency

| Severity | Issue | Location | Impact |
|---|---|---|---|
| Low | `frontend/package.json` does not include `dompurify` or any HTML sanitiser. Acceptable today because no `innerHTML`-style rendering exists, but the moment metadata-driven UI (mentioned at the bottom of `CLAUDE.md:195-199`) starts injecting HTML from API metadata, a sanitiser will be needed. | `frontend/package.json` (no sanitiser dep) | Preventive flag — not exploitable today. |
| Low | The `index.html` template was not read in detail; whether it uses subresource integrity (SRI) for any external scripts is not confirmed. The Vite default is no SRI on its own emitted assets. | `frontend/index.html` (not deeply audited) | Mostly relevant if external CDNs are added later. |

---

## 3. Recommendations

### Immediate
- Add a strict baseline CSP to `frontend/nginx.conf.template`:
  ```nginx
  add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'" always;
  add_header X-Content-Type-Options nosniff always;
  add_header X-Frame-Options DENY always;
  add_header Referrer-Policy strict-origin-when-cross-origin always;
  ```
- Mirror equivalent headers via Spring's `HeadersConfigurer` so direct backend access also carries them.

### Short-term
- Add `dompurify` as a dependency before the metadata-driven renderer lands.
- Add an HTML-injection regression test that asserts `<script>` from a server payload renders inert.

### Long-term
- Move toward CSP nonces for any inline `style`/`script` Vite emits.
- Add SRI for any third-party assets loaded outside the Vite-bundled graph.

---

## Conclusion

For the current surface (forms + simple data tables + token-bearing API calls) the UI is **safe by construction**. The two Medium findings are pre-emptive: add CSP and an HTML sanitiser before metadata-driven rendering arrives, because once it does, the cost of retrofitting these controls rises.
