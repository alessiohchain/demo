import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright config — runs against the live `docker compose` stack
 * (demo frontend on :8082, backend on :8092) with the platform IdP up on
 * :8090. Sign-in is the central IdP's OIDC flow, so the platform stack +
 * the registered DEMO module/clients must be running too.
 *
 * Single worker by default: specs share one signed-in session via the
 * `setup` project, and parallel workers would race on shared dev data.
 *
 * The `setup` project signs in ONCE (auth.setup.ts) and persists the session;
 * the chromium project loads that storage state and silently re-auths per
 * test via the IdP session cookie — so only the setup hits the throttled
 * `/login` endpoint.
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
    { name: 'setup', testMatch: /auth\.setup\.ts/ },
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/state.json',
      },
      dependencies: ['setup'],
    },
  ],
});
