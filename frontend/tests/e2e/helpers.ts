import { expect, type Page } from '@playwright/test';

/** Type a fastpath into the header input and press Enter. */
export async function fastpathTo(page: Page, code: string): Promise<void> {
  await page.getByRole('textbox', { name: /fastpath/i }).fill(code);
  // Click the autocomplete option (faster than Enter, avoids race with dropdown).
  const option = page.getByRole('option', { name: new RegExp(`^${code}\\b`) });
  if (await option.count()) {
    await option.first().click();
  } else {
    await page.getByRole('textbox', { name: /fastpath/i }).press('Enter');
  }
  await expect(page).toHaveURL(new RegExp(`/${code}$`));
}

/** Click a toolbar button by its stable {@code data-button-name}. */
export async function clickToolbarButton(page: Page, name: string): Promise<void> {
  await page.locator(`button[data-button-name="${name}"]`).first().click();
}

/** Submit the Search criteria dialog that opens when `cmd_search` is local. */
export async function submitSearchDialog(page: Page): Promise<void> {
  const dialog = page.getByRole('dialog', { name: /search/i });
  await dialog.getByRole('button', { name: /^search$/i }).click();
  await expect(dialog).not.toBeVisible();
}

/**
 * Run a local `cmd_search`: open the criteria dialog if it isn't already
 * showing, then submit it. List screens (e.g. COSF) need the toolbar click
 * to open the dialog; picker/prompt screens (e.g. the TRDP trader picker)
 * auto-open it on entry, so the toolbar click would land on the already-open
 * modal and time out. This helper tolerates both: it waits briefly for an
 * auto-opened dialog and only clicks `cmd_search` when none appears.
 */
export async function runSearch(page: Page): Promise<void> {
  const dialog = page.getByRole('dialog', { name: /search/i });
  try {
    await dialog.waitFor({ state: 'visible', timeout: 1200 });
  } catch {
    await clickToolbarButton(page, 'cmd_search');
    await dialog.waitFor({ state: 'visible', timeout: 5000 });
  }
  await dialog.getByRole('button', { name: /^search$/i }).click();
  await expect(dialog).not.toBeVisible();
}

/** Tick the checkbox on the grid row whose first non-checkbox cell matches. */
export async function selectGridRowByText(page: Page, text: string): Promise<void> {
  const row = page.locator('tbody tr', { hasText: text }).first();
  await row.locator('input[type="checkbox"]').check();
}

/** Click the "Yes" / confirm button on the destructive-action confirm dialog. */
export async function confirmDialog(page: Page): Promise<void> {
  await page.getByRole('dialog').getByRole('button', { name: /^(yes|ok|confirm)$/i }).click();
}

/** Wait for any `/api/process` request matching `command` to settle. */
export async function expectProcessCommand(page: Page, command: string): Promise<void> {
  await page.waitForResponse(
    (r) =>
      r.url().includes('/api/process')
      && r.request().method() === 'POST'
      && (r.request().postData() ?? '').includes(`"command":"${command}"`),
  );
}
