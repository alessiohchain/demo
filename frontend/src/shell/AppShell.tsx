import { EngineAppShell } from '@alessiohchain/csnx-engine/shell/EngineAppShell';
import { ThemePicker } from '@/app/theme/ThemePicker';

/**
 * Demo's shell = the shared {@link EngineAppShell} with just the theme picker
 * injected. Demo has no smart-nav assistant or operator-messages inbox, so it
 * omits those slots; the brand, module switcher, fastpath, warehouse switcher,
 * user menu, sidebar and breadcrumb all come from the engine. The session that
 * feeds it is wired in {@code main.tsx}.
 */
export function AppShell() {
  return <EngineAppShell themeControl={<ThemePicker />} />;
}
