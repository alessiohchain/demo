import { test as base, expect, type Page } from '@playwright/test';

/**
 * Per-test login through the central platform IdP (module-local login is
 * retired). Visiting any protected route auto-redirects into the OIDC
 * code flow; the styled IdP login page at :8090/login takes the
 * credentials and bounces back authenticated.
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
    // RequireAuth fires the OIDC redirect → styled IdP login page.
    await expect(page).toHaveURL(/:8090\/login/);
    // Company auto-defaults once /api/lookup/init resolves; the submit
    // stays disabled until then.
    await page.getByLabel(/username/i).fill(process.env.WCS_USERNAME ?? 'wcs');
    await page.getByLabel(/^password$/i).fill(process.env.WCS_PASSWORD ?? 'wcs123!');
    const signIn = page.getByRole('button', { name: /sign in/i });
    await expect(signIn).toBeEnabled();
    await signIn.click();
    // Code flow completes via /auth/callback and restores the deep link.
    await expect(page).toHaveURL(/\/$/);
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
