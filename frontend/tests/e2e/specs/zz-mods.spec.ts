import { test, expect } from '../fixture';

/**
 * MODS — the shared EngineAppShell module switcher + the platform portal.
 *
 * Exercises the whole sign-in/switch feature end to end against the live
 * stacks: the access-token `modules` claim drives the in-header switcher; a
 * pick navigates cross-origin to the target module, which silently re-auths
 * via the shared IdP session cookie; and the platform portal renders branded
 * module tiles. Needs platform :8090/:8091 + POM :8081 + demo :8082 all up.
 *
 * IMPORTANT — this file is named `zz-` so it runs LAST. Its cross-origin
 * auth against other clients (pom-module / portal) advances the shared IdP
 * session that the auth-reuse `storageState` froze at setup time; in a real
 * browser the Set-Cookie keeps the session current, but Playwright replays
 * the frozen cookie per test, so any spec that ran AFTER these would find a
 * stale session and bounce to /login. Keeping these last avoids corrupting
 * the shared session for the rest of the suite.
 */
test.describe('MODS — module switcher + portal', () => {
  test('the switcher lists every module the user can reach', async ({ authedPage: page }) => {
    await page.getByRole('button', { name: 'Open module launcher' }).click();
    await expect(page.getByRole('menuitem', { name: /Demo Module/ })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: /Purchase Order Management/ })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: /Platform Admin/ })).toBeVisible();
  });

  test('switching DEMO → POM lands authenticated on the POM origin', async ({ authedPage: page }) => {
    await page.getByRole('button', { name: 'Open module launcher' }).click();
    await page.getByRole('menuitem', { name: /Purchase Order Management/ }).click();
    // Cross-origin navigation to POM; the shared IdP cookie makes re-auth silent.
    await expect(page).toHaveURL(/localhost:8081/, { timeout: 20_000 });
    await expect(page.getByRole('button', { name: /sign out/i })).toBeVisible({ timeout: 20_000 });
    // POM's switcher shows its own launcher — confirms the shell mounted.
    await expect(page.getByRole('button', { name: 'Open module launcher' })).toBeVisible();
  });

  test('the platform portal renders branded module tiles', async ({ authedPage: page }) => {
    await page.goto('http://localhost:8091/');
    await expect(page.getByRole('button', { name: /sign out/i })).toBeVisible({ timeout: 20_000 });

    // Platform Admin has smart-nav disabled (no engine-ai backend), so the
    // shared shell hides the assistant field; the version label shows 1.0.
    await expect(page.getByRole('textbox', { name: /smart navigation/i })).toHaveCount(0);
    await expect(page.getByText('1.0', { exact: true }).first()).toBeVisible();

    // The portal lists the user's launchable modules as branded tiles. (The
    // portal's own module tile is hidden — it would only link back here; and
    // favorites/recents were removed, so it's a plain module grid now.)
    await expect(page.getByRole('link', { name: /Purchase Order Management/ }).first()).toBeVisible();
    await expect(page.getByRole('link', { name: /Demo Module/ }).first()).toBeVisible();
  });
});
