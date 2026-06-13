import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright config — runs against the live `docker compose` stack
 * (demo frontend on :8082, backend on :8092) with the platform IdP up on
 * :8090. Sign-in is the central IdP's OIDC flow, so the platform stack +
 * the registered DEMO module/clients must be running too.
 *
 * Single worker by default: the test fixture logs in via the UI per spec,
 * and parallel workers can race for the same wcs/WCS demo user.
 *
 * To run: `npm run test:e2e`.
 */
export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8082',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    actionTimeout: 10_000,
    navigationTimeout: 15_000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
