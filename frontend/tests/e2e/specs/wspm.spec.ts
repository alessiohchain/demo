import { test, expect } from '../fixture';
import { clickToolbarButton, fastpathTo } from '../helpers';

test.describe('WSPM — System Parameters', () => {
  test('loads the company singleton row and persists edits', async ({
    authedPage: page,
  }) => {
    // Singleton-edit screen: cmd_search returns the WCS company's one
    // row as a form envelope; cmd_update upserts. Integer inputs render
    // as <input type="number"> which Playwright sees as role=spinbutton.
    await fastpathTo(page, 'WSPM');

    // Confirm collapsible section headers render. Fields are tagged with
    // {@code sectionName} in the screen JSON; DynamicForm.groupBySection
    // wraps each named section in a shadcn Collapsible.
    await expect(page.getByRole('button', { name: 'System' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Receiving' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Verification' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Orders & Shipments' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Delivery Notes' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'RF Devices' })).toBeVisible();

    // Wait for the form fields to render. Don't assert the initial
    // values — prior test runs may have edited them and DB state
    // persists across runs in dev.
    const dpw = page.getByRole('spinbutton', { name: /days per week/i });
    const rwt = page.getByRole('spinbutton', { name: /received weight tolerance/i });
    const vml = page.getByRole('spinbutton', { name: /min verification length/i });
    await expect(dpw).toBeVisible();
    await expect(rwt).toBeVisible();
    await expect(vml).toBeVisible();

    // Edit one field per logical section. Press Tab after each fill to
    // force the controlled input to commit.
    await dpw.fill('6');
    await dpw.press('Tab');
    await rwt.fill('7');
    await rwt.press('Tab');
    await vml.fill('5');
    await vml.press('Tab');

    const updateResp = page.waitForResponse((r) =>
      r.url().includes('/api/process')
      && (r.request().postData() ?? '').includes('"command":"cmd_update"')
      && (r.request().postData() ?? '').includes('"sysParameters.maintenance"'),
    );
    await clickToolbarButton(page, 'cmd_update');
    const resp = await updateResp;
    const body = await resp.json();
    expect(body.exception, JSON.stringify(body.exception)).toBeFalsy();

    // Server echoes the saved row back as a form envelope.
    const saved = body.modelHolders?.['']?.model?.data as Record<string, unknown> | undefined;
    expect(saved, 'cmd_update must return saved row in modelHolders[""].model').toBeTruthy();
    expect(Number(saved!.daysPerWeek)).toBe(6);
    expect(Number(saved!.receivedWeightTolerance)).toBe(7);
    expect(Number(saved!.verificationMinLength)).toBe(5);
    expect(saved!.maintenanceTran).toBe('WSPM');

    // Re-fastpath to confirm persistence after a fresh load.
    await fastpathTo(page, 'WSPM');
    await expect(page.getByRole('spinbutton', { name: /days per week/i }))
        .toHaveValue('6');
  });

  test('rejects inverted verification min/max with a server-side validation error', async ({
    authedPage: page,
  }) => {
    // Server-only inter-field rule: verificationMaxLength must be >=
    // verificationMinLength. The client has no equivalent constraint, so
    // the request fires and the backend's beforeUpdate rejects.
    await fastpathTo(page, 'WSPM');
    await expect(page.getByRole('spinbutton', { name: /min verification length/i })).toBeVisible();

    const min = page.getByRole('spinbutton', { name: /min verification length/i });
    const max = page.getByRole('spinbutton', { name: /max verification length/i });
    await min.fill('20');
    await min.press('Tab');
    await max.fill('10');
    await max.press('Tab');

    const resp = page.waitForResponse((r) =>
      r.url().includes('/api/process')
      && (r.request().postData() ?? '').includes('"command":"cmd_update"')
      && (r.request().postData() ?? '').includes('"sysParameters.maintenance"'),
    );
    await clickToolbarButton(page, 'cmd_update');
    const r = await resp;
    const body = await r.json();
    expect(body.exception,
      'expected BusinessException on inverted verification lengths').toBeTruthy();
    expect(JSON.stringify(body.exception)).toContain('Verification max length');
  });
});
