import { test, expect } from '../fixture';
import {
  clickToolbarButton,
  fastpathTo,
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

    // Delete + confirm. Ticking a grid checkbox updates the engine's
    // selectedRows on the NEXT React tick, so a cmd_delete fired in the same
    // microtask can read an empty selection and never open the confirm dialog.
    // Retry select → delete until the confirm dialog appears, then confirm.
    const confirmDelete = page.getByRole('dialog').getByRole('button', { name: /^delete$/i });
    await expect(async () => {
      if (await confirmDelete.isVisible()) return; // dialog already open
      const cb = page.locator('tbody tr', { hasText: flowCode }).first().locator('input[type="checkbox"]');
      await cb.check();
      await expect(cb).toBeChecked();
      await clickToolbarButton(page, 'cmd_delete');
      await expect(confirmDelete).toBeVisible({ timeout: 3000 });
    }).toPass({ timeout: 15_000 });
    await confirmDelete.click();

    // Refresh search; row should be gone.
    await clickToolbarButton(page, 'cmd_search');
    await submitSearchDialog(page);
    await expect(page.locator('tbody tr', { hasText: flowCode })).toHaveCount(0);
  });
});
