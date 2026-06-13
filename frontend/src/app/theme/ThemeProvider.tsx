import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react';
import { ACCENTS, DEFAULT_ACCENT, DEFAULT_MODE, getAccent } from '@/app/theme/themes';
import type { AccentId, Mode } from '@/app/theme/themes';

/**
 * Theme persistence — user-scoped localStorage key so each operator who
 * logs in on a shared workstation keeps their own preference.
 *
 * Anonymous (pre-login) sessions write to {@code _shared} so the login
 * screen still respects the last operator's pick; once authenticated
 * the user's own key takes over.
 */
const STORAGE_KEY_PREFIX = 'csnx.theme';

function storageKey(username: string | undefined): string {
  return `${STORAGE_KEY_PREFIX}/${username && username.length > 0 ? username : '_shared'}`;
}

interface Stored {
  accent?: AccentId;
  mode?: Mode;
}

function readStored(username: string | undefined): Stored {
  try {
    const raw = localStorage.getItem(storageKey(username));
    if (!raw) return {};
    const parsed = JSON.parse(raw) as Stored;
    return parsed ?? {};
  } catch {
    return {};
  }
}

function writeStored(username: string | undefined, next: Stored) {
  try {
    localStorage.setItem(storageKey(username), JSON.stringify(next));
  } catch {
    // localStorage may be unavailable (privacy mode, full disk). The
    // running session still reflects the choice — just no persistence.
  }
}

interface ThemeContextValue {
  accent: AccentId;
  mode: Mode;
  /** Resolved mode after collapsing {@code 'system'} via the
   *  {@code prefers-color-scheme} media query. */
  effectiveMode: 'light' | 'dark';
  setAccent: (id: AccentId) => void;
  setMode: (m: Mode) => void;
  reset: () => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

function applyTheme(accent: AccentId, effective: 'light' | 'dark') {
  const accentDef = getAccent(accent);
  const tokens = effective === 'dark' ? accentDef.dark : accentDef.light;

  const root = document.documentElement;
  root.style.setProperty('--primary', tokens.primary);
  root.style.setProperty('--primary-foreground', tokens.primaryForeground);
  root.style.setProperty('--ring', tokens.ring);

  // Toggle dark-mode class on <html> so Tailwind's `dark:` variants kick
  // in alongside the CSS-variable swap.
  root.classList.toggle('dark', effective === 'dark');

  // Expose the active accent id on <html> for any CSS that wants to
  // branch on it (status chips, accent-tinted icons).
  root.dataset.accent = accent;
}

interface ProviderProps extends PropsWithChildren {
  /** Optional username — picker preferences are scoped per user so a
   *  shared workstation keeps each operator's choice separate. */
  username?: string;
}

/**
 * Loads stored accent + mode for the given user (or the shared bucket
 * pre-login), applies the corresponding CSS variables, and exposes a
 * stable context to the picker. Re-reads when {@code username} changes
 * so logging in / out swaps to that user's preference without a reload.
 */
export function ThemeProvider({ children, username }: ProviderProps) {
  const [accent, setAccentState] = useState<AccentId>(() => {
    return readStored(username).accent ?? DEFAULT_ACCENT;
  });
  const [mode, setModeState] = useState<Mode>(() => {
    return readStored(username).mode ?? DEFAULT_MODE;
  });
  const [systemPrefersDark, setSystemPrefersDark] = useState<boolean>(() => {
    if (typeof window === 'undefined') return false;
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
  });

  // Re-read when the active user changes (post-login, post-logout). The
  // user's own bucket may carry different choices to the shared one.
  useEffect(() => {
    const stored = readStored(username);
    setAccentState(stored.accent ?? DEFAULT_ACCENT);
    setModeState(stored.mode ?? DEFAULT_MODE);
  }, [username]);

  // Listen to the OS-level dark-mode preference so users on
  // {@code mode: 'system'} flip with the system theme without
  // re-loading.
  useEffect(() => {
    if (typeof window === 'undefined') return;
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (e: MediaQueryListEvent) => setSystemPrefersDark(e.matches);
    mq.addEventListener?.('change', onChange);
    return () => mq.removeEventListener?.('change', onChange);
  }, []);

  const effectiveMode = useMemo<'light' | 'dark'>(() => {
    if (mode === 'system') return systemPrefersDark ? 'dark' : 'light';
    return mode;
  }, [mode, systemPrefersDark]);

  // Apply CSS vars + the {@code dark} class whenever accent/mode resolves.
  useEffect(() => {
    applyTheme(accent, effectiveMode);
  }, [accent, effectiveMode]);

  const setAccent = useCallback(
    (id: AccentId) => {
      setAccentState(id);
      writeStored(username, { accent: id, mode });
    },
    [username, mode],
  );

  const setMode = useCallback(
    (m: Mode) => {
      setModeState(m);
      writeStored(username, { accent, mode: m });
    },
    [username, accent],
  );

  const reset = useCallback(() => {
    setAccentState(DEFAULT_ACCENT);
    setModeState(DEFAULT_MODE);
    writeStored(username, { accent: DEFAULT_ACCENT, mode: DEFAULT_MODE });
  }, [username]);

  const value = useMemo<ThemeContextValue>(
    () => ({ accent, mode, effectiveMode, setAccent, setMode, reset }),
    [accent, mode, effectiveMode, setAccent, setMode, reset],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used inside <ThemeProvider>');
  return ctx;
}

export { ACCENTS };
export type { AccentId, Mode };
