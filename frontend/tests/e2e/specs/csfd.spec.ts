import type { Page } from '@playwright/test';
import { test, expect } from '../fixture';
import {
  clickToolbarButton,
  fastpathTo,
  runSearch,
  selectGridRowByText,
} from '../helpers';

test.describe('CSFD — Corporate Shipment Flow Details', () => {
  test('Details button navigates with header context + populated detail grid', async ({
    authedPage: page,
  }) => {
    await fastpathTo(page, 'COSF');
    await runSearch(page);

    await selectGridRowByText(page, 'A1');
    await clickToolbarButton(page, 'cmd_details');

    await expect(page).toHaveURL(/\/CSFD$/);

    // Header form shows the parent context (disabled).
    await expect(page.locator('input[data-field-name="shipmentFlow"]').first()).toHaveValue('A1');
    await expect(page.locator('input[data-field-name="flowDescription"]').first()).toHaveValue('Main');

    // Detail grid populated with the 3 seeded child rows. The add-via-
    // picker spec leaves an extra WH-510 row behind on a prior run (no
    // engine-level delete-by-id available), so use .first() to tolerate
    // multiplicity.
    await expect(page.locator('tbody tr', { hasText: 'WH-101' }).first()).toBeVisible();
    await expect(page.locator('tbody tr', { hasText: 'WH-510' }).first()).toBeVisible();
    await expect(page.locator('tbody tr', { hasText: 'C-001' }).first()).toBeVisible();
  });

  test('Add detail row using trader picker', async ({ authedPage: page }) => {
    // Drill into CSFD for A1
    await fastpathTo(page, 'COSF');
    await runSearch(page);
    await selectGridRowByText(page, 'A1');
    await clickToolbarButton(page, 'cmd_details');
    await expect(page).toHaveURL(/\/CSFD$/);

    const initialRowCount = await page.locator('tbody tr').filter({ hasNotText: 'No rows.' }).count();

    // Open Add dialog
    await clickToolbarButton(page, 'cmd_create');
    const addDialog = page.getByRole('dialog', { name: 'Add' });
    await expect(addDialog.getByText(/^trader\*?$/i)).toBeVisible();

    // Open trader picker via the prompt button next to the Trader field.
    await addDialog.getByRole('button', { name: /open lookup/i }).click();
    await expect(page).toHaveURL(/\/TRDP$/);

    // Search the picker for all traders
    await runSearch(page);
    await expect(page.locator('tbody tr', { hasText: 'WH-510' })).toBeVisible();

    // Pick WH-510 by clicking its cell (clickForDetail).
    await page.locator('tbody tr', { hasText: 'WH-510' }).locator('td', { hasText: 'WH-510' }).click();

    // Returned to CSFD with Add dialog re-opened, fields populated.
    await expect(page).toHaveURL(/\/CSFD$/);
    const reopenedDialog = page.getByRole('dialog', { name: 'Add' });
    await expect(reopenedDialog.locator('input[data-field-name="traderCode"]')).toHaveValue('WH-510');

    // Trader Type combobox should show 'Warehouse'.
    await expect(reopenedDialog.getByText('Warehouse', { exact: true })).toBeVisible();

    // Fill the numeric times.
    const numInputs = reopenedDialog.locator('input[type="number"]');
    await numInputs.nth(0).fill('3');
    await numInputs.nth(1).fill('2');

    // Save — server returns the saved row under modelHolders[""].model
    // (singular). EntityDialog's master-detail branch appends it to the
    // children grid client-side; no manual Search needed.
    const createResponse = page.waitForResponse((r) =>
      r.url().includes('/api/process')
      && (r.request().postData() ?? '').includes('"command":"cmd_create"')
      && (r.request().postData() ?? '').includes('"corporateShipmentFlowDetails.maintenance"'),
    );
    await reopenedDialog.getByRole('button', { name: /save/i }).click();
    const resp = await createResponse;
    const body = await resp.json();
    expect(body.exception, JSON.stringify(body.exception)).toBeFalsy();

    const holder = body.modelHolders?.[''];
    const savedRow = holder?.model?.data as Record<string, unknown> | undefined;
    expect(savedRow, 'cmd_create must return saved row in modelHolders[""].model').toBeTruthy();
    expect(savedRow!.shipmentFlow).toBe('A1');
    expect(savedRow!.traderCode).toBe('WH-510');
    expect(savedRow!.traderType).toBe('W');
    expect(savedRow!.processTime).toBe(2);
    expect(savedRow!.transitTime).toBe(3);
    expect(Number(savedRow!.shipmentFlowSeq)).toBeGreaterThanOrEqual(40);

    // The detail grid should now show the new row WITHOUT manually clicking
    // Search — the engine's master-detail append branch fires off the
    // singleton response.
    await expect(reopenedDialog).not.toBeVisible();
    await expect(
      page.locator('tbody tr', { hasText: 'WH-510' }).filter({ hasText: /Warehouse 510/ }).last(),
    ).toBeVisible();
  });

  test('Inline-edit Update on a just-added row replaces it in-place (no duplicate)', async ({
    authedPage: page,
  }) => {
    // Regression: EntityDialog appends the freshly-saved row to the
    // children grid keyless (the cmd_create response has no rowID — it's
    // a client-side grid concept). A subsequent inline-edit Update on
    // that same row used to ship the row's data back without a rowID,
    // the engine's mergeRowsByKey couldn't match it, and the saved row
    // was appended a SECOND time — the user saw the row twice.
    // Fix: setMasterDetail stamps a fresh rowID on any keyless child.
    await fastpathTo(page, 'COSF');
    await runSearch(page);
    await selectGridRowByText(page, 'A1');
    await clickToolbarButton(page, 'cmd_details');
    await expect(page).toHaveURL(/\/CSFD$/);

    // Open Add dialog and create a row via the trader picker. Pick
    // C-002 (a trader NOT in the seeded A1 detail list) so the new row
    // is unambiguously identifiable.
    await clickToolbarButton(page, 'cmd_create');
    const addDialog = page.getByRole('dialog', { name: 'Add' });
    await addDialog.getByRole('button', { name: /open lookup/i }).click();
    await expect(page).toHaveURL(/\/TRDP$/);
    await runSearch(page);
    await page.locator('tbody tr', { hasText: 'C-002' })
        .locator('td', { hasText: 'C-002' }).click();
    await expect(page).toHaveURL(/\/CSFD$/);
    const reopenedDialog = page.getByRole('dialog', { name: 'Add' });
    await expect(reopenedDialog.locator('input[data-field-name="traderCode"]')).toHaveValue('C-002');
    const nums = reopenedDialog.locator('input[type="number"]');
    await nums.nth(0).fill('1');
    await nums.nth(1).fill('1');
    await reopenedDialog.getByRole('button', { name: /save/i }).click();
    await expect(reopenedDialog).not.toBeVisible();

    // Wait for the new C-002 row to appear in the grid. (Prior test
    // runs leave extra C-002 rows behind — no engine-level delete-by-id
    // — so just confirm at least one exists.)
    await expect(page.locator('tbody tr', { hasText: 'C-002' }).first()).toBeVisible();

    // Count C-002 rows pre-update — could be >1 from prior runs.
    const c002CountBefore = await page.locator('tbody tr', { hasText: 'C-002' }).count();

    // Select the new row via its checkbox and click Update. We don't
    // need to actually modify a field — the bug is purely about whether
    // the freshly-appended row carries a rowID. Without it, the
    // backend's cmd_update response also has no rowID, the engine's
    // mergeRowsByKey can't find a match, and the row gets *appended* a
    // second time (the duplicate). The selected-rows-without-edits
    // path is a valid Update — see performInlineUpdate's gate.
    const c002Row = page.locator('tbody tr', { hasText: 'C-002' }).last();
    await c002Row.locator('input[type="checkbox"]').check();

    const updateResponse = page.waitForResponse((r) =>
      r.url().includes('/api/process')
      && (r.request().postData() ?? '').includes('"command":"cmd_update"')
      && (r.request().postData() ?? '').includes('"corporateShipmentFlowDetails.maintenance"'),
    );
    await clickToolbarButton(page, 'cmd_update');
    const resp = await updateResponse;
    const body = await resp.json();
    expect(body.exception, JSON.stringify(body.exception)).toBeFalsy();

    // Give the grid a beat to settle, then assert no duplicate appeared.
    await page.waitForTimeout(300);
    const c002CountAfter = await page.locator('tbody tr', { hasText: 'C-002' }).count();
    expect(c002CountAfter).toBe(c002CountBefore);
  });

  test('Copy of an existing row with changed trader succeeds (no duplicate-key error)', async ({
    authedPage: page,
  }) => {
    // Regression: cmd_copy used to fail with "Record already exists with that
    // key" because the dialog seeded shipmentFlowId from the source row and
    // the activity's validateDuplicate found that exact id already in the DB.
    // Fix: strip shipmentFlowId in withInsertContext before the insert.
    await fastpathTo(page, 'COSF');
    await runSearch(page);
    await selectGridRowByText(page, 'A1');
    await clickToolbarButton(page, 'cmd_details');
    await expect(page).toHaveURL(/\/CSFD$/);

    // Select an existing detail row (the seeded WH-101 row with seq 10).
    await page.locator('tbody tr', { hasText: 'WH-101' }).first()
        .locator('input[type="checkbox"]').check();

    // Fire cmd_copy via the toolbar.
    await clickToolbarButton(page, 'cmd_copy');
    const copyDialog = page.getByRole('dialog', { name: /copy/i });
    await expect(copyDialog).toBeVisible();
    // Source row pre-filled WH-101 / Warehouse. Change to NORTH / Supplier.
    await copyDialog.locator('input[data-field-name="traderCode"]').fill('NORTH');
    // The traderType combobox in the dialog needs to be set to S. The
    // demo combobox is a Radix Select; click then choose by visible label.
    await copyDialog.getByRole('combobox', { name: /trader type/i }).click();
    await page.getByRole('option', { name: 'Supplier' }).click();

    // The Save inside a Copy dialog fires whichever command the engine
    // wires up (cmd_copy or cmd_create depending on dialog wiring) —
    // match either so the test isn't brittle to that detail.
    const copyResponse = page.waitForResponse((r) =>
      r.url().includes('/api/process')
      && /"command":"(cmd_copy|cmd_create)"/.test(r.request().postData() ?? '')
      && (r.request().postData() ?? '').includes('"NORTH"'),
    );
    await copyDialog.getByRole('button', { name: /save/i }).click();
    const resp = await copyResponse;
    const body = await resp.json();
    expect(
      body.exception,
      'cmd_copy should not raise BusinessException — ' + JSON.stringify(body.exception),
    ).toBeFalsy();

    // Detail grid auto-refreshes via parentChild envelope.
    await expect(copyDialog).not.toBeVisible();
    await expect(page.locator('tbody tr', { hasText: 'NORTH' }).first()).toBeVisible();
  });

  test('First Add to an empty children grid appends the row immediately', async ({
    authedPage: page,
  }) => {
    // Regression: COSF's cmd_details for a flow with ZERO children used
    // to ship modelHolders[''].models = [] under componentType
    // 'parentChild'. The forward-nav handler's isFlatParentChild check
    // required flatModels.length > 0, so children landed undefined and
    // the destination CSFD seeded with no parentChild slot. EntityDialog's
    // submit handler then read masterDetail=null, took the !preMasterDetail
    // branch, and dropped the first saved row on the floor — the user
    // had to navigate away and back to see it. Fix: componentType =
    // 'parentChild' is authoritative regardless of child count.
    const flow = `TFA_${Date.now()}`;

    // Create a fresh flow in COSF.
    await fastpathTo(page, 'COSF');
    await clickToolbarButton(page, 'cmd_create');
    const cosfAdd = page.getByRole('dialog', { name: 'Add' });
    await cosfAdd.locator('input[data-field-name="shipmentFlow"]').fill(flow);
    await cosfAdd.locator('input[data-field-name="flowDescription"]').fill('First-add regression');
    await cosfAdd.getByRole('button', { name: /save/i }).click();
    await expect(cosfAdd).not.toBeVisible();

    // Search + select + Details on the new flow (zero children).
    await runSearch(page);
    await selectGridRowByText(page, flow);
    await clickToolbarButton(page, 'cmd_details');
    await expect(page).toHaveURL(/\/CSFD$/);

    // Children grid must be empty up front.
    await expect(page.locator('tbody tr', { hasText: 'No rows' })).toBeVisible();

    // Add the first child via the trader picker.
    await clickToolbarButton(page, 'cmd_create');
    const csfdAdd = page.getByRole('dialog', { name: 'Add' });
    await csfdAdd.getByRole('button', { name: /open lookup/i }).click();
    await expect(page).toHaveURL(/\/TRDP$/);
    await runSearch(page);
    await page.locator('tbody tr', { hasText: 'WH-510' })
        .locator('td', { hasText: 'WH-510' }).click();
    await expect(page).toHaveURL(/\/CSFD$/);

    const reopened = page.getByRole('dialog', { name: 'Add' });
    await expect(reopened.locator('input[data-field-name="traderCode"]')).toHaveValue('WH-510');
    const nums = reopened.locator('input[type="number"]');
    await nums.nth(0).fill('5');
    await nums.nth(1).fill('6');
    await reopened.getByRole('button', { name: /save/i }).click();
    await expect(reopened).not.toBeVisible();

    // The just-saved WH-510 row must be visible in the grid without
    // requiring a manual Search. The bug used to leave the grid empty
    // here.
    const newRow = page.locator('tbody tr', { hasText: 'WH-510' }).first();
    await expect(newRow).toBeVisible();
    await expect(page.locator('tbody tr', { hasText: 'No rows' })).not.toBeVisible();

    // Regression: the Trader Name column must be populated immediately —
    // the bean used to ship the saved entity with @Transient traderName
    // null, leaving the column blank until cmd_search re-ran the
    // trader-name join. Fixed by hydrating traderName in validate().
    await expect(newRow).toContainText('Warehouse 510');

    await cleanupFlow(page, flow);
  });

  test('Delete of the LAST remaining child row clears the grid', async ({
    authedPage: page,
  }) => {
    // Regression: CSFD's cmd_delete server-side returns a
    // refreshedChildren envelope — componentType "parentChild",
    // actionType "none", models = [] when nothing is left. The frontend's
    // applyResponse default branch used to treat empty models as
    // "no change to this slot" unless mode was strict — so the
    // just-deleted last row stayed visible. Fix: recognise an
    // explicit componentType=parentChild as authoritative for the slot
    // regardless of child count.
    const flow = `TDL_${Date.now()}`;

    // Setup: create a flow with exactly one detail row.
    await fastpathTo(page, 'COSF');
    await clickToolbarButton(page, 'cmd_create');
    const cosfAdd = page.getByRole('dialog', { name: 'Add' });
    await cosfAdd.locator('input[data-field-name="shipmentFlow"]').fill(flow);
    await cosfAdd.locator('input[data-field-name="flowDescription"]').fill('Delete-last regression');
    await cosfAdd.getByRole('button', { name: /save/i }).click();
    await expect(cosfAdd).not.toBeVisible();

    await runSearch(page);
    await selectGridRowByText(page, flow);
    await clickToolbarButton(page, 'cmd_details');
    await expect(page).toHaveURL(/\/CSFD$/);

    await clickToolbarButton(page, 'cmd_create');
    const csfdAdd = page.getByRole('dialog', { name: 'Add' });
    await csfdAdd.getByRole('button', { name: /open lookup/i }).click();
    await runSearch(page);
    await page.locator('tbody tr', { hasText: 'WH-101' })
        .locator('td', { hasText: 'WH-101' }).click();
    const reopened = page.getByRole('dialog', { name: 'Add' });
    const nums = reopened.locator('input[type="number"]');
    await nums.nth(0).fill('1');
    await nums.nth(1).fill('1');
    await reopened.getByRole('button', { name: /save/i }).click();
    await expect(reopened).not.toBeVisible();
    await expect(page.locator('tbody tr', { hasText: 'WH-101' }).first()).toBeVisible();

    // Tick the only row and Delete.
    await page.locator('tbody tr', { hasText: 'WH-101' }).first()
        .locator('input[type="checkbox"]').check();
    await clickToolbarButton(page, 'cmd_delete');
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 3000 });
    const deleteResp = page.waitForResponse((r) =>
      r.url().includes('/api/process')
      && (r.request().postData() ?? '').includes('"command":"cmd_delete"'),
    );
    await page.getByRole('dialog').getByRole('button', { name: /^delete$/i }).click();
    const resp = await deleteResp;
    const body = await resp.json();
    expect(body.exception, JSON.stringify(body.exception)).toBeFalsy();

    // Grid must show "No rows" — the deleted last row should NOT linger.
    await expect(page.locator('tbody tr', { hasText: 'No rows' })).toBeVisible();
    await expect(page.locator('tbody tr', { hasText: 'WH-101' })).toHaveCount(0);

    await cleanupFlow(page, flow);
  });

  test('Delete prompts for confirmation and removes the row from the grid', async ({
    authedPage: page,
  }) => {
    // Regression: master-detail screens have a toolbar bypass that sends
    // selection-based non-local buttons straight to performGenericServerCall,
    // which skips the confirm dialog and fires the request immediately.
    // The bypass must special-case destructive verbs (cmd_delete,
    // cmd_remove, cmd_deletes) so the confirm dialog still opens.
    //
    // Hermetic via a fresh flow + fresh child — does NOT touch the
    // seeded A1 row set so the suite is repeatable.
    const flow = `TDC_${Date.now()}`;
    await createFlowWithOneChild(page, flow, 'C-001');

    // Tick the only row and click Delete.
    await page.locator('tbody tr', { hasText: 'C-001' }).first()
        .locator('input[type="checkbox"]').check();

    // Click Delete — the toolbar's master-detail bypass now keeps the
    // confirm dialog for destructive verbs, so cmd_delete on CSFD pops
    // the confirm modal instead of firing straight at the server.
    await clickToolbarButton(page, 'cmd_delete');
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 3000 });

    // Confirm — the actual cmd_delete request fires from performDelete.
    const deleteResp = page.waitForResponse((r) =>
      r.url().includes('/api/process')
      && (r.request().postData() ?? '').includes('"command":"cmd_delete"'),
    );
    await page.getByRole('dialog').getByRole('button', { name: /^delete$/i }).click();
    const resp = await deleteResp;
    const body = await resp.json();
    expect(body.exception, JSON.stringify(body.exception)).toBeFalsy();

    // The just-deleted row should disappear from the grid.
    await page.waitForTimeout(400);
    await expect(page.locator('tbody tr', { hasText: 'C-001' })).toHaveCount(0);

    await cleanupFlow(page, flow);
  });
});

/** Create a fresh flow header via COSF, navigate into CSFD, add one
 *  detail row keyed on {@code traderCode} (an existing seeded trader),
 *  and leave the user on CSFD with the new row visible. */
async function createFlowWithOneChild(page: Page, flow: string, traderCode: string): Promise<void> {
  await fastpathTo(page, 'COSF');
  await clickToolbarButton(page, 'cmd_create');
  const cosfAdd = page.getByRole('dialog', { name: 'Add' });
  await cosfAdd.locator('input[data-field-name="shipmentFlow"]').fill(flow);
  await cosfAdd.locator('input[data-field-name="flowDescription"]').fill(`Test flow ${flow}`);
  await cosfAdd.getByRole('button', { name: /save/i }).click();
  await expect(cosfAdd).not.toBeVisible();

  await runSearch(page);
  await selectGridRowByText(page, flow);
  await clickToolbarButton(page, 'cmd_details');
  await expect(page).toHaveURL(/\/CSFD$/);

  await clickToolbarButton(page, 'cmd_create');
  const csfdAdd = page.getByRole('dialog', { name: 'Add' });
  await csfdAdd.getByRole('button', { name: /open lookup/i }).click();
  await runSearch(page);
  await page.locator('tbody tr', { hasText: traderCode })
      .locator('td', { hasText: traderCode }).click();
  const reopened = page.getByRole('dialog', { name: 'Add' });
  const nums = reopened.locator('input[type="number"]');
  await nums.nth(0).fill('1');
  await nums.nth(1).fill('1');
  await reopened.getByRole('button', { name: /save/i }).click();
  await expect(reopened).not.toBeVisible();
  await expect(page.locator('tbody tr', { hasText: traderCode }).first()).toBeVisible();
}

/** Navigate to COSF, search, select the test flow, delete it (cascades
 *  children server-side). Leaves no header/detail artifacts. */
async function cleanupFlow(page: Page, flow: string): Promise<void> {
  await fastpathTo(page, 'COSF');
  await runSearch(page);
  const row = page.locator('tbody tr', { hasText: flow });
  if (await row.count() === 0) return;
  await row.first().locator('input[type="checkbox"]').check();
  await clickToolbarButton(page, 'cmd_delete');
  const confirmDlg = page.getByRole('dialog');
  if (await confirmDlg.count() > 0) {
    const confirmBtn = confirmDlg.getByRole('button', { name: /^delete$/i });
    if (await confirmBtn.count() > 0) await confirmBtn.click();
  }
}
