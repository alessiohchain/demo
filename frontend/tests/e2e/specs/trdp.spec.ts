import { test, expect } from '../fixture';
import {
  clickToolbarButton,
  fastpathTo,
  selectGridRowByText,
  submitSearchDialog,
} from '../helpers';

/**
 * Regression: clicking the trader-code column in the picker must return
 * the picked row to the parent screen (CSFD) just like clicking any other
 * cell. The bug was {@code clickForDetail: true} on the traderCode column
 * intercepting the click and firing {@code cmd_detail} on the read-only
 * picker workflow — silent no-op, no return navigation. Fix: drop
 * {@code clickForDetail} from the picker JSON so every cell click bubbles
 * to the row's onClick which performs the return.
 *
 * @see docs/engine.md §20.6
 */
test.describe('TRDP — Trader picker return paths', () => {
  test.beforeEach(async ({ authedPage: page }) => {
    // Get into the trader-picker via CSFD → Add → Open lookup.
    await fastpathTo(page, 'COSF');
    await clickToolbarButton(page, 'cmd_search');
    await submitSearchDialog(page);
    await selectGridRowByText(page, 'A1');
    await clickToolbarButton(page, 'cmd_details');
    await expect(page).toHaveURL(/\/CSFD$/);
    await clickToolbarButton(page, 'cmd_create');
    await page.getByRole('dialog', { name: 'Add' })
        .getByRole('button', { name: /open lookup/i })
        .click();
    await expect(page).toHaveURL(/\/TRDP$/);
    await clickToolbarButton(page, 'cmd_search');
    await submitSearchDialog(page);
    await expect(page.locator('tbody tr', { hasText: 'WH-510' })).toBeVisible();
  });

  test('clicking the traderCode cell returns the picked trader', async ({ authedPage: page }) => {
    // Click EXACTLY on the trader-code cell. Without the fix the cell
    // had clickForDetail: true and the click no-op'd silently.
    const traderRow = page.locator('tbody tr', { hasText: 'WH-510' });
    await traderRow.locator('td', { hasText: 'WH-510' }).click();

    await expect(page).toHaveURL(/\/CSFD$/);
    const reopened = page.getByRole('dialog', { name: 'Add' });
    await expect(reopened.locator('input[data-field-name="traderCode"]')).toHaveValue('WH-510');
    await expect(reopened.getByText('Warehouse', { exact: true })).toBeVisible();
  });

  test('clicking the traderName cell also returns the picked trader', async ({ authedPage: page }) => {
    // Other cells (traderName, traderType) hit the row's onClick directly —
    // a working path even before the fix; covered here as a regression guard
    // so a future change can't silently break it.
    const traderRow = page.locator('tbody tr', { hasText: 'Customer Two Wholesale' });
    await traderRow.locator('td', { hasText: 'Customer Two Wholesale' }).click();

    await expect(page).toHaveURL(/\/CSFD$/);
    const reopened = page.getByRole('dialog', { name: 'Add' });
    await expect(reopened.locator('input[data-field-name="traderCode"]')).toHaveValue('C-002');
    await expect(reopened.getByText('Customer', { exact: true })).toBeVisible();
  });

  test('clicking the traderType cell also returns the picked trader', async ({ authedPage: page }) => {
    // The traderType column shows the VVD label ("Warehouse" not "W"), so
    // text matching has to use the label.
    const traderRow = page.locator('tbody tr', { hasText: 'ACME' });
    await traderRow.locator('td', { hasText: 'Supplier' }).first().click();

    await expect(page).toHaveURL(/\/CSFD$/);
    const reopened = page.getByRole('dialog', { name: 'Add' });
    await expect(reopened.locator('input[data-field-name="traderCode"]')).toHaveValue('ACME');
    await expect(reopened.getByText('Supplier', { exact: true })).toBeVisible();
  });
});
