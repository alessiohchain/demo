/**
 * Theme registry — 8 accent palettes, each with light + dark HSL values
 * for the CSS variables shadcn/ui reads. Slate is the "current default"
 * (matches the unaltered shadcn baseline). The other seven recolour
 * {@code --primary}, {@code --primary-foreground}, and {@code --ring}
 * on top of the slate base; all other tokens (background, card, muted,
 * border, ...) stay identical so contrast and density never shift when
 * the user picks a new accent.
 *
 * HSL values are stored as raw "H S% L%" strings — the form shadcn
 * variables expect ({@code hsl(var(--primary))}). The {@code swatch}
 * value is the on-canvas chip colour and matches the dark-mode primary
 * so the picker tile reads the same on light and dark backgrounds.
 */

export type AccentId =
  | 'slate'
  | 'teal'
  | 'pine'
  | 'indigo'
  | 'electric-blue'
  | 'operator-orange'
  | 'plum'
  | 'magenta';

export type Mode = 'light' | 'dark' | 'system';

export interface ThemeTokens {
  primary: string;
  primaryForeground: string;
  ring: string;
}

export interface AccentDef {
  id: AccentId;
  name: string;
  /** Short tag-line shown under the swatch grid when this accent is
   *  selected (e.g. "Default", "Recommended"). */
  tag?: string;
  /** Solid chip colour for the picker tile — matches the on-screen
   *  primary so users see the actual accent, not a normalised hue. */
  swatch: string;
  light: ThemeTokens;
  dark: ThemeTokens;
}

export const DEFAULT_ACCENT: AccentId = 'slate';
export const DEFAULT_MODE: Mode = 'system';

export const ACCENTS: AccentDef[] = [
  {
    id: 'teal',
    name: 'Teal',
    tag: 'Recommended',
    swatch: '#0d9488',
    light: {
      primary: '173 80% 32%',
      primaryForeground: '0 0% 100%',
      ring: '173 80% 32%',
    },
    dark: {
      primary: '172 66% 50%',
      primaryForeground: '173 80% 10%',
      ring: '172 66% 50%',
    },
  },
  {
    id: 'pine',
    name: 'Pine',
    swatch: '#0f766e',
    light: {
      primary: '161 94% 24%',
      primaryForeground: '0 0% 100%',
      ring: '161 94% 24%',
    },
    dark: {
      primary: '160 84% 39%',
      primaryForeground: '160 84% 10%',
      ring: '160 84% 39%',
    },
  },
  {
    id: 'indigo',
    name: 'Indigo',
    swatch: '#6366f1',
    light: {
      primary: '239 84% 60%',
      primaryForeground: '0 0% 100%',
      ring: '239 84% 60%',
    },
    dark: {
      primary: '234 89% 74%',
      primaryForeground: '243 75% 12%',
      ring: '234 89% 74%',
    },
  },
  {
    id: 'electric-blue',
    name: 'Electric Blue',
    swatch: '#2563eb',
    light: {
      primary: '217 91% 52%',
      primaryForeground: '0 0% 100%',
      ring: '217 91% 52%',
    },
    dark: {
      primary: '213 94% 68%',
      primaryForeground: '217 91% 12%',
      ring: '213 94% 68%',
    },
  },
  {
    id: 'operator-orange',
    name: 'Operator Orange',
    swatch: '#ea580c',
    light: {
      primary: '21 90% 48%',
      primaryForeground: '0 0% 100%',
      ring: '21 90% 48%',
    },
    dark: {
      primary: '27 96% 61%',
      primaryForeground: '21 90% 12%',
      ring: '27 96% 61%',
    },
  },
  {
    id: 'plum',
    name: 'Plum',
    swatch: '#9333ea',
    light: {
      primary: '271 76% 48%',
      primaryForeground: '0 0% 100%',
      ring: '271 76% 48%',
    },
    dark: {
      primary: '270 91% 70%',
      primaryForeground: '271 76% 12%',
      ring: '270 91% 70%',
    },
  },
  {
    id: 'magenta',
    name: 'Magenta',
    swatch: '#db2777',
    light: {
      primary: '330 81% 50%',
      primaryForeground: '0 0% 100%',
      ring: '330 81% 50%',
    },
    dark: {
      primary: '328 86% 70%',
      primaryForeground: '330 81% 12%',
      ring: '328 86% 70%',
    },
  },
  {
    id: 'slate',
    name: 'Slate',
    tag: 'Default',
    swatch: '#0f172a',
    light: {
      primary: '222.2 47.4% 11.2%',
      primaryForeground: '210 40% 98%',
      ring: '222.2 84% 4.9%',
    },
    dark: {
      primary: '210 40% 98%',
      primaryForeground: '222.2 47.4% 11.2%',
      ring: '212.7 26.8% 83.9%',
    },
  },
];

export function getAccent(id: AccentId | string | undefined): AccentDef {
  return ACCENTS.find((a) => a.id === id) ?? ACCENTS.find((a) => a.id === DEFAULT_ACCENT)!;
}
