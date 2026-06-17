---
genre: infrastructure
category: accessibility
analysis-type: static
relevance:
  file-patterns:
    - "**/components/**"
    - "**/views/**"
    - "**/pages/**"
  keywords:
    - "aria"
    - "wcag"
    - "a11y"
    - "accessibility"
    - "screen-reader"
  config-keys:
    - "eslint-plugin-jsx-a11y"
    - "axe-core"
    - "pa11y"
  always-include: false
severity-scale: "Critical|High|Medium|Low|Info"
---

# Accessibility Maturity

**Assessment Date:** 2026-05-26
**Status:** Complete

---

<!-- analysis: static -->

## Maturity score — **3 / 5 (Functional)**

The frontend uses Radix UI primitives, which carry strong baseline ARIA behaviour, and the sampled login page uses semantic markup (`<label htmlFor>`, `<button>`, `aria-hidden` on decorative icons). The maturity gap is **no a11y tooling in the pipeline** (`eslint-plugin-jsx-a11y` absent, no axe / pa11y tests).

| Item | Present? | Source |
|---|---|---|
| Radix UI primitives | Yes (15 packages — Dialog, Dropdown, Select, Tooltip, etc.) | `frontend/package.json:14-27` |
| shadcn-style wrappers around Radix | Yes (`frontend/src/components/ui/*.tsx`) | `frontend/src/components/ui/` |
| `eslint-plugin-jsx-a11y` | **No** | `frontend/eslint.config.js` |
| `axe-core` / `@axe-core/react` | **No** | `frontend/package.json` |
| `pa11y` / `lighthouse-ci` | **No** | (no CI exists anyway) |
| Storybook with a11y addon | **No** | — |
| `aria-*` usage in sampled code | Yes — `aria-hidden` on decorative icons, `aria-label` on selects, `aria-invalid` doc'd via shadcn `Field` | `frontend/src/shell/Login.tsx:115, 130, 137, 180`; `CLAUDE.md:139-142` |
| Form labels associated with controls | Yes (`htmlFor`) | `frontend/src/shell/Login.tsx:161-165, 184-198, 200-214` |
| Keyboard focus visible | Yes — `focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2` | `Login.tsx:219` |
| Skip-link / landmark roles | Not observed in `shell/` | — |

---

## Static observations

### Strengths
- Login form has correctly associated labels via `htmlFor` (`Login.tsx:161, 184, 200`).
- Decorative icons are marked `aria-hidden` (multiple sites in `Login.tsx`).
- `ThemedSelect` exposes `aria-label="Company"` (`Login.tsx:180`).
- Radix primitives carry strong baseline ARIA semantics (Dialog `aria-modal`, Menu `role="menu"` etc.).
- Convention in `CLAUDE.md:139-142` requires using `Field` primitive which surfaces validation via `data-invalid` + `aria-invalid`.

### Gaps
- No skip-link / `<main>` landmark in `AppShell.tsx` (the file was not read in detail; flag for manual confirmation).
- Mobile-first responsive convention is documented (`CLAUDE.md:109-122`) but no automated viewport test confirms it.
- The branded panel in `Login.tsx:92-141` has CSS-only decorative elements (`<div aria-hidden ...>`) — correctly marked.

---

## Findings

| Severity | Issue | Location | Recommendation |
|---|---|---|---|
| Medium | No `eslint-plugin-jsx-a11y`. Common a11y mistakes (missing `alt`, `<div>` with `onClick` and no role, missing `htmlFor`) ship undetected. | `frontend/eslint.config.js` | Add the plugin + the `recommended` config. |
| Medium | No automated a11y test (axe / pa11y / Storybook a11y addon). | `frontend/package.json` (no test framework at all) | Once Vitest lands (per `front-end.md`), add `@axe-core/react` smoke tests on the main pages. |
| Low | No visible `<main>` landmark / skip-link audit in `shell/AppShell.tsx`. | `frontend/src/shell/AppShell.tsx` (not deeply audited) | Manual review; ensure `<main role="main">` wraps the routed content. |
| Info | Mobile-responsive design is well-documented but not test-enforced. | `CLAUDE.md:109-122` | Add a Playwright viewport test once a test runner exists. |

---

## Maturity verdict — **3**

The Radix-first approach gives the demo a strong a11y baseline by construction. Adding the lint plugin + a single axe-based smoke test would lift this to 4.
