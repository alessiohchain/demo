import { test, expect } from '../fixture';

/**
 * SMARTNAV — the shared smart-nav assistant (now rendered by the engine shell,
 * gated by features.smartNavigation) + the real version label.
 *
 * Demo has smartNavigation enabled in its module_config AND engine-ai wired, so
 * the assistant field renders here and its /api/assistant endpoint is reachable
 * (FeatureGate allows it). The header version label shows the configured version
 * (1.0), not the module code — the switcher already identifies the module.
 */
test.describe('SMARTNAV — shared assistant + version label', () => {
  test('the smart-nav assistant field renders on DEMO', async ({ authedPage: page }) => {
    await expect(page.getByRole('textbox', { name: /smart navigation/i })).toBeVisible();
  });

  test('the header version label shows the real version (1.0), not the module code', async ({
    authedPage: page,
  }) => {
    // versionInfo now sources csnx.module.version (default 1.0), shown next to
    // the module switcher (which is what identifies the module).
    await expect(page.getByText('1.0', { exact: true })).toBeVisible();
  });

  test('the /api/assistant endpoint is wired on DEMO (FeatureGate allows it — not 403/404)', async ({
    authedPage: page,
  }) => {
    const field = page.getByRole('textbox', { name: /smart navigation/i });
    await field.fill('show all corporate shipment flows');
    const respPromise = page.waitForResponse(
      (r) => r.url().includes('/api/assistant') && r.request().method() === 'POST',
      { timeout: 30_000 },
    );
    await field.press('Enter');
    const resp = await respPromise;
    // 200 = the endpoint processed the request (the LLM result itself — navigate
    // vs. a graceful "busy" toast on a quota 429 — is not asserted here, only
    // that engine-ai is wired and the feature is enabled).
    expect(resp.status()).toBe(200);
  });
});
