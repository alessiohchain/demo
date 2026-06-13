import { test, expect } from '../fixture';
import {
  clickToolbarButton,
  fastpathTo,
  selectGridRowByText,
  submitSearchDialog,
} from '../helpers';

test.describe('COSF — Corporate Shipment Flows', () => {
  test('search returns seeded headers', async ({ authedPage: page }) => {
    await fastpathTo(page, 'COSF');
    await clickToolbarButton(page, 'cmd_search');
    await submitSearchDialog(page);

    // Two seeded headers: A1 (Main) and STORE1 (Test through a store).
    await expect(page.locator('tbody tr', { hasText: 'A1' })).toBeVisible();
    await expect(page.locator('tbody tr', { hasText: 'STORE1' })).toBeVisible();
  });

  test('add + delete a header round-trips', async ({ authedPage: page }) => {
    const flowCode = `PWTEST_${Date.now().toString().slice(-6)}`;

    await fastpathTo(page, 'COSF');

    // Add
    await clickToolbarButton(page, 'cmd_create');
    const addDialog = page.getByRole('dialog', { name: 'Add' });
    await addDialog.locator('input[data-field-name="shipmentFlow"]').fill(flowCode);
    await addDialog.locator('input[data-field-name="flowDescription"]').fill('Playwright test row');
    await addDialog.getByRole('button', { name: /save/i }).click();
    await expect(addDialog).not.toBeVisible();

    // Search to find it
    await clickToolbarButton(page, 'cmd_search');
    await submitSearchDialog(page);
    const newRow = page.locator('tbody tr', { hasText: flowCode });
    await expect(newRow).toBeVisible();

    // Delete + confirm
    await selectGridRowByText(page, flowCode);
    await clickToolbarButton(page, 'cmd_delete');
    await page.getByRole('dialog').getByRole('button', { name: /^delete$/i }).click();

    // Refresh search; row should be gone.
    await clickToolbarButton(page, 'cmd_search');
    await submitSearchDialog(page);
    await expect(page.locator('tbody tr', { hasText: flowCode })).toHaveCount(0);
  });
});
