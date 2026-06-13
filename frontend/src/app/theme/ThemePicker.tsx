import * as Popover from '@radix-ui/react-popover';
import { Check, ChevronDown, Monitor, Moon, RotateCcw, Sun } from 'lucide-react';
import { ACCENTS, useTheme } from '@/app/theme/ThemeProvider';
import type { Mode } from '@/app/theme/themes';
import { cn } from '@/lib/utils';

/**
 * Header-mounted accent + mode picker. The trigger shows a coloured dot
 * matching the active accent and the label "Theme"; clicking it opens a
 * small popover with:
 *
 *   1. ACCENT COLOUR — 4×2 grid of swatches. Selected one shows a ring
 *      and a check mark; clicking any swatch applies and persists.
 *   2. APPEARANCE   — Light / Dark / System segmented toggle.
 *   3. Footer       — Reset link + "Saved automatically" reassurance.
 *
 * The popover is intentionally lightweight — no Save button, no
 * confirmation. Every click is immediate and persisted to localStorage
 * under the active user's bucket, so the choice survives logout and
 * re-login.
 */
export function ThemePicker() {
  const { accent, mode, setAccent, setMode, reset } = useTheme();
  const active = ACCENTS.find((a) => a.id === accent) ?? ACCENTS[ACCENTS.length - 1];

  return (
    <Popover.Root>
      <Popover.Trigger asChild>
        <button
          type="button"
          aria-label="Theme settings"
          className="inline-flex items-center gap-2 rounded-md border bg-background px-3 h-8 text-sm transition-colors hover:border-primary hover:bg-primary/5 hover:text-primary"
        >
          <span
            aria-hidden
            className="h-3 w-3 rounded-full ring-1 ring-border"
            style={{ backgroundColor: active.swatch }}
          />
          Theme
          <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" aria-hidden />
        </button>
      </Popover.Trigger>
      <Popover.Portal>
        <Popover.Content
          align="end"
          sideOffset={6}
          className="z-50 w-[20rem] rounded-md border bg-popover text-popover-foreground shadow-lg"
        >
          <div className="p-4">
            <div className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
              Accent colour
            </div>
            <div className="mt-3 grid grid-cols-4 gap-2">
              {ACCENTS.map((a) => {
                const selected = a.id === accent;
                return (
                  <button
                    key={a.id}
                    type="button"
                    title={a.name}
                    aria-label={`${a.name}${a.tag ? ` — ${a.tag}` : ''}`}
                    aria-pressed={selected}
                    onClick={() => setAccent(a.id)}
                    className={cn(
                      'group relative aspect-square rounded-md transition-shadow',
                      selected
                        ? 'ring-2 ring-ring ring-offset-2 ring-offset-popover'
                        : 'hover:ring-2 hover:ring-border hover:ring-offset-2 hover:ring-offset-popover',
                    )}
                    style={{ backgroundColor: a.swatch }}
                  >
                    {selected && (
                      <Check
                        className="absolute inset-0 m-auto h-5 w-5 text-white drop-shadow-sm"
                        aria-hidden
                      />
                    )}
                  </button>
                );
              })}
            </div>
            <div className="mt-3 flex items-center gap-1.5 text-xs">
              {active.tag === 'Recommended' && (
                <span className="inline-flex items-center gap-1 text-muted-foreground">
                  <span
                    aria-hidden
                    className="h-1.5 w-1.5 rounded-full"
                    style={{ backgroundColor: active.swatch }}
                  />
                  Recommended
                </span>
              )}
              <span className="font-mono text-muted-foreground">{active.name}</span>
              {active.tag && active.tag !== 'Recommended' && (
                <span className="text-muted-foreground">— {active.tag}</span>
              )}
            </div>
          </div>

          <div className="border-t px-4 py-4">
            <div className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
              Appearance
            </div>
            <div className="mt-3 grid grid-cols-3 rounded-md border bg-muted/30 p-1 text-sm">
              <ModeButton current={mode} value="light" onSelect={setMode} icon={<Sun className="h-3.5 w-3.5" />}>
                Light
              </ModeButton>
              <ModeButton current={mode} value="dark" onSelect={setMode} icon={<Moon className="h-3.5 w-3.5" />}>
                Dark
              </ModeButton>
              <ModeButton
                current={mode}
                value="system"
                onSelect={setMode}
                icon={<Monitor className="h-3.5 w-3.5" />}
              >
                System
              </ModeButton>
            </div>
          </div>

          <div className="flex items-center justify-between border-t px-4 py-3 text-xs">
            <button
              type="button"
              onClick={reset}
              className="inline-flex items-center gap-1.5 text-muted-foreground hover:text-foreground"
            >
              <RotateCcw className="h-3.5 w-3.5" aria-hidden />
              Reset to default
            </button>
            <span className="text-muted-foreground">Saved automatically</span>
          </div>
        </Popover.Content>
      </Popover.Portal>
    </Popover.Root>
  );
}

function ModeButton({
  current,
  value,
  onSelect,
  icon,
  children,
}: {
  current: Mode;
  value: Mode;
  onSelect: (m: Mode) => void;
  icon: React.ReactNode;
  children: React.ReactNode;
}) {
  const selected = current === value;
  return (
    <button
      type="button"
      aria-pressed={selected}
      onClick={() => onSelect(value)}
      className={cn(
        'inline-flex items-center justify-center gap-1.5 rounded-sm px-2 py-1 transition-colors',
        selected
          ? 'bg-background shadow-xs text-foreground'
          : 'text-muted-foreground hover:text-foreground',
      )}
    >
      {icon}
      <span>{children}</span>
    </button>
  );
}
