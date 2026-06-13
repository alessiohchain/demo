import { test as base, expect, type Page } from '@playwright/test';

/**
 * Authenticated page fixture. Sign-in itself happens ONCE in auth.setup.ts;
 * each spec's browser context starts from that persisted storage state (see
 * playwright.config.ts), so visiting a protected route silently re-auths via
 * the IdP session cookie (OIDC code flow with no /login prompt) and lands
 * authenticated — no per-test credential entry, no /login rate-limit churn.
 *
 * Prereq: the platform stack must be running on :8090 (docker compose up
 * in C:\software\projects\modules\platform) alongside the demo stack, and
 * the DEMO module + demo-module OAuth2 client must be registered there
 * (platform migration V32 + the demo-module/demo-service clients).
 *
 * The fixture also sniffs the Authorization header off outbound /api
 * calls so API-driven helpers can reuse the in-memory access token
 * without re-authenticating.
 */
export const test = base.extend<{ authedPage: Page }>({
  authedPage: async ({ page }, use) => {
    // Capture the bearer token the SPA attaches to API calls.
    page.on('request', (req) => {
      const auth = req.headers()['authorization'];
      if (auth?.startsWith('Bearer ')) {
        (page as Page & { __bearer?: string }).__bearer = auth.substring('Bearer '.length);
      }
    });

    await page.goto('/');
    // The access token lives in memory, so a fresh context has none — the SPA
    // re-runs the OIDC code flow, but the persisted IdP session cookie makes
    // it silent (no /login). Wait for the authenticated shell.
    await expect(page.getByRole('button', { name: /sign out/i })).toBeVisible({ timeout: 15_000 });
    await use(page);
  },
});

/** The access token sniffed off the authed page's API traffic. */
export function bearerOf(page: Page): string {
  const token = (page as Page & { __bearer?: string }).__bearer;
  if (!token) {
    throw new Error('No bearer captured yet — perform any API-backed action first.');
  }
  return token;
}

export { expect };
