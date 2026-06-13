import { forwardRef, type ReactNode } from 'react';
import * as Select from '@radix-ui/react-select';
import { Check, ChevronDown, ChevronUp } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface ThemedSelectOption {
  /** Value sent over the wire. CSnx convention: the empty string ""
   *  represents "no selection" — pass {@code blankOption} for that. */
  value: string;
  /** Human-readable label rendered in the trigger and dropdown. */
  label: string;
  /** Optional small secondary label (right-aligned, monospace) — used
   *  for codes alongside descriptions. */
  hint?: string;
  /** When true, item is unselectable; rendered greyed out. */
  disabled?: boolean;
}

interface Props {
  value: string;
  onValueChange: (v: string) => void;
  options: ThemedSelectOption[];
  /** Optional blank/clear option rendered at the top of the list.
   *  Selecting it emits {@code ""} via {@link onValueChange}.
   *  Pass {@code "Any"} (search popups) or a hint like {@code "All"}. */
  blankOption?: { label?: string } | null;
  disabled?: boolean;
  /** Empty-value placeholder. */
  placeholder?: string;
  /** Outer trigger className override (sizing, border colour overrides). */
  className?: string;
  /** Visual variant. */
  variant?: 'default' | 'invalid';
  id?: string;
  name?: string;
  /** Native form-control values for accessibility / form submission. */
  required?: boolean;
  /** Optional id used on the trigger so a `<label htmlFor>` outside the
   *  component can still associate. */
  triggerId?: string;
  /** ARIA label when no visible label is associated. */
  'aria-label'?: string;
}

/**
 * Theme-aware select built on Radix Select. Replaces the browser-native
 * `<select>` so option-hover and selected styling read in the active
 * accent — native `<option>` elements ignore CSS in every browser, so
 * a true themed dropdown demands a portal-rendered listbox like this.
 *
 * <p>Defaults match the project's other input controls: {@code h-9},
 * rounded-md, accent focus ring. Hover and "highlighted" items tint
 * to {@code bg-primary/10 text-primary} — same pattern grid rows and
 * the warehouse / user menus use.
 *
 * <p>Empty {@code ""} value is supported via the {@code blankOption}
 * prop. Radix can't accept an empty-string {@code value} on a
 * {@code SelectItem}, so we represent blank with the sentinel
 * {@code __blank__} and translate at the boundaries. The caller still
 * sees {@code ""} both into and out of the component.
 */
const BLANK_SENTINEL = '__blank__';

export const ThemedSelect = forwardRef<HTMLButtonElement, Props>(function ThemedSelect(
  {
    value,
    onValueChange,
    options,
    blankOption,
    disabled,
    placeholder,
    className,
    variant = 'default',
    id,
    name,
    required,
    triggerId,
    ...rest
  },
  ref,
) {
  const radixValue = value === '' ? BLANK_SENTINEL : value;
  const handleValueChange = (v: string) => {
    onValueChange(v === BLANK_SENTINEL ? '' : v);
  };

  return (
    <Select.Root
      value={radixValue}
      onValueChange={handleValueChange}
      disabled={disabled}
      name={name}
      required={required}
    >
      <Select.Trigger
        ref={ref}
        id={triggerId ?? id}
        aria-label={rest['aria-label']}
        className={cn(
          'flex h-9 w-full items-center justify-between gap-2 rounded-md border border-input bg-background px-3 py-1 text-sm shadow-xs transition-colors',
          'focus:outline-hidden focus:border-ring focus:ring-2 focus:ring-ring/30',
          'disabled:bg-muted disabled:text-muted-foreground disabled:border-muted disabled:cursor-not-allowed',
          variant === 'invalid' && 'border-destructive focus:border-destructive focus:ring-destructive/30',
          className,
        )}
      >
        <Select.Value placeholder={placeholder ?? ''} />
        <Select.Icon asChild>
          <ChevronDown className="h-3.5 w-3.5 text-muted-foreground shrink-0" aria-hidden />
        </Select.Icon>
      </Select.Trigger>
      <Select.Portal>
        <Select.Content
          position="popper"
          sideOffset={4}
          // {@code --radix-select-trigger-width} lets the popper match
          // the trigger width exactly so the dropdown lines up under
          // the input rather than collapsing to its content length.
          className="z-50 min-w-(--radix-select-trigger-width) overflow-hidden rounded-md border bg-popover text-popover-foreground shadow-md"
        >
          <Select.ScrollUpButton className="flex h-6 items-center justify-center bg-popover">
            <ChevronUp className="h-3.5 w-3.5" aria-hidden />
          </Select.ScrollUpButton>
          <Select.Viewport className="p-1 max-h-72">
            {blankOption && (
              <ThemedSelectItem value={BLANK_SENTINEL} label={blankOption.label ?? ''} />
            )}
            {options.map((o) => {
              // Radix forbids an empty-string {@code value} on a
              // {@code Select.Item} — it reserves "" for clearing the
              // selection and showing the placeholder. Some CSnx
              // lookups (e.g. WSPM's optional codes) ship a blank
              // entry inline rather than via {@code blankOption}, so
              // remap any empty-string value to the same
              // {@code __blank__} sentinel the {@code blankOption}
              // branch uses. The {@code onValueChange} adapter at the
              // {@code Select.Root} level translates the sentinel
              // back to {@code ""} on the way out, so callers still
              // see the legacy empty-string contract.
              const itemValue = o.value === '' ? BLANK_SENTINEL : o.value;
              return (
                <ThemedSelectItem
                  key={itemValue}
                  value={itemValue}
                  label={o.label}
                  hint={o.hint}
                  disabled={o.disabled}
                />
              );
            })}
          </Select.Viewport>
          <Select.ScrollDownButton className="flex h-6 items-center justify-center bg-popover">
            <ChevronDown className="h-3.5 w-3.5" aria-hidden />
          </Select.ScrollDownButton>
        </Select.Content>
      </Select.Portal>
    </Select.Root>
  );
});

function ThemedSelectItem({
  value,
  label,
  hint,
  disabled,
}: {
  value: string;
  label: ReactNode;
  hint?: string;
  disabled?: boolean;
}) {
  return (
    <Select.Item
      value={value}
      disabled={disabled}
      // Radix sets {@code data-highlighted} on the currently-focused /
      // hovered item and {@code data-state="checked"} on the selected
      // one. Tint both with the active accent so the theme shows up in
      // the dropdown the same way it does in grid rows and the
      // warehouse / user menus.
      className={cn(
        'flex w-full cursor-default items-center justify-between gap-3 rounded-sm px-2 py-1.5 text-sm outline-hidden transition-colors',
        'data-highlighted:bg-primary/10 data-highlighted:text-primary',
        'data-[state=checked]:bg-primary/10 data-[state=checked]:text-primary data-[state=checked]:font-medium',
        'data-disabled:pointer-events-none data-disabled:opacity-50',
      )}
    >
      {/* {@code Select.ItemText} provides the value rendered inside the
          trigger when this item is the selected one. For the blank
          option CSnx renders nothing — leave the item visually empty
          rather than substituting an em-dash so a blank selection
          reads identically to the legacy GWT UI. The row stays
          interactable thanks to the surrounding padding. */}
      <Select.ItemText>
        <span className="flex-1 truncate">{label}</span>
      </Select.ItemText>
      <span className="flex items-center gap-2">
        {hint && <span className="font-mono text-xs text-muted-foreground tabular-nums">{hint}</span>}
        <Select.ItemIndicator>
          <Check className="h-3.5 w-3.5" aria-hidden />
        </Select.ItemIndicator>
      </span>
    </Select.Item>
  );
}
