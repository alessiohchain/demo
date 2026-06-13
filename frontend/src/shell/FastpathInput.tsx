import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type FormEvent,
  type KeyboardEvent,
} from 'react';
import { useNavigate } from 'react-router-dom';
import { Search } from 'lucide-react';
import { useAuth, type MenuItem } from '@/app/auth/AuthProvider';
import { cn } from '@/lib/utils';
import { useQueryClient } from '@tanstack/react-query';
import { clearBreadcrumbs } from '@alessiohchain/csnx-engine/breadcrumbs/breadcrumbStore';
import { resetWorkflowHistory } from '@alessiohchain/csnx-engine/process/workflowTracker';
import { startFreshScreenFlow } from '@alessiohchain/csnx-engine/screen/screenFlow';

/**
 * CSNX-13939 — header fastpath input with typeahead, mirroring the GWT app.
 *
 * <p>Types-as-you-go filtering against the user's accessible fastpaths.
 * Source: the hierarchical menu (descriptions live there) joined with the
 * flat {@code fastpaths} map (covers orphan codes the user can reach but
 * that aren't in the visible menu tree).
 *
 * <p>Keyboard:
 *   - {@code Enter} → navigate to the highlighted suggestion, or fall back
 *     to the typed text as a literal fastpath
 *   - {@code ↑ / ↓} → move the highlight
 *   - {@code Esc} → close the suggestions dropdown
 */
interface FastpathOption {
  code: string;
  description: string;
}

const MAX_VISIBLE = 8;

export function FastpathInput() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { menu, fastpaths } = useAuth();
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [highlight, setHighlight] = useState(0);
  const containerRef = useRef<HTMLFormElement>(null);

  const allOptions = useMemo(() => buildOptions(menu, fastpaths), [menu, fastpaths]);

  const filtered = useMemo(() => filterOptions(allOptions, query), [allOptions, query]);
  const visible = filtered.slice(0, MAX_VISIBLE);

  // Reset highlight when the filter changes so it never points off the end.
  useEffect(() => {
    setHighlight(0);
  }, [query]);

  // Click outside to close.
  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (!containerRef.current) return;
      if (!containerRef.current.contains(e.target as Node)) setOpen(false);
    }
    if (open) document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, [open]);

  function navigateTo(code: string) {
    const trimmed = code.trim().toUpperCase();
    if (!trimmed) return;
    setQuery('');
    setOpen(false);
    // Fastpath entry is a fresh top-level workflow start — same semantics
    // as a menu click. Mirrors GWT's
    // {@code changePage(workflow, true, true)}: clear the breadcrumb
    // trail, wipe the workflow tracker (so a Close on the destination
    // doesn't try to navigate(-1) into the previous flow), and replace
    // the current browser entry so the prior screen leaves the back-
    // stack entirely. End result: typing a fastpath reliably starts
    // fresh; Close from the destination lands on /.
    clearBreadcrumbs();
    resetWorkflowHistory();
    startFreshScreenFlow(queryClient);
    navigate(`/${trimmed}`, { replace: true });
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    const target = visible[highlight]?.code ?? query;
    navigateTo(target);
  }

  function onKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setOpen(true);
      setHighlight((h) => Math.min(visible.length - 1, h + 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlight((h) => Math.max(0, h - 1));
    } else if (e.key === 'Escape') {
      setOpen(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="relative" ref={containerRef}>
      <div className="relative">
        <Search className="absolute left-2 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" aria-hidden />
        <input
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={onKeyDown}
          placeholder="Fastpath"
          className="h-8 w-40 rounded-md border border-input bg-background pl-8 pr-2 text-sm uppercase tracking-wide transition-all hover:border-primary hover:bg-primary/5 focus:w-56 focus:outline-hidden focus:border-ring focus:bg-background focus:ring-2 focus:ring-ring/30"
          maxLength={20}
          autoComplete="off"
          spellCheck={false}
          aria-label="Fastpath"
          aria-expanded={open}
          aria-autocomplete="list"
        />
      </div>

      {open && visible.length > 0 && (
        <ul
          role="listbox"
          className="absolute right-0 mt-1 w-72 max-h-80 overflow-auto rounded-md border bg-popover text-popover-foreground shadow-lg z-30"
        >
          {visible.map((opt, idx) => (
            <li
              key={opt.code}
              role="option"
              aria-selected={idx === highlight}
              onMouseDown={(e) => {
                // Use mousedown rather than click so the input doesn't lose
                // focus and re-render the list before we get the value.
                e.preventDefault();
                navigateTo(opt.code);
              }}
              onMouseEnter={() => setHighlight(idx)}
              className={cn(
                'flex items-baseline justify-between gap-3 px-3 py-2 cursor-pointer text-sm transition-colors',
                idx === highlight
                  ? 'bg-primary/10 text-primary'
                  : 'hover:bg-primary/5',
              )}
            >
              <span className="font-mono text-xs font-semibold tracking-wide">{opt.code}</span>
              <span className="flex-1 truncate text-muted-foreground">{opt.description}</span>
            </li>
          ))}
          {filtered.length > MAX_VISIBLE && (
            <li className="px-3 py-1.5 text-xs text-muted-foreground italic">
              … and {filtered.length - MAX_VISIBLE} more
            </li>
          )}
        </ul>
      )}
    </form>
  );
}

function buildOptions(menu: MenuItem[], fastpaths: Record<string, string>): FastpathOption[] {
  const seen = new Map<string, FastpathOption>();
  const walk = (nodes: MenuItem[] | undefined) => {
    if (!nodes) return;
    for (const node of nodes) {
      if (node.fastpath) {
        const code = node.fastpath.toUpperCase();
        if (!seen.has(code)) {
          seen.set(code, { code, description: node.description });
        }
      }
      walk(node.children);
    }
  };
  walk(menu);

  // Add orphan fastpaths (in the flat map but not in the visible menu).
  for (const code of Object.keys(fastpaths)) {
    const upper = code.toUpperCase();
    if (!seen.has(upper)) {
      seen.set(upper, { code: upper, description: fastpaths[code] ?? upper });
    }
  }
  return Array.from(seen.values()).sort((a, b) => a.code.localeCompare(b.code));
}

function filterOptions(options: FastpathOption[], rawQuery: string): FastpathOption[] {
  const query = rawQuery.trim().toUpperCase();
  if (!query) return options;
  // Prefix match on the code is the strongest signal — surface those first,
  // then substring matches on description.
  const codePrefix: FastpathOption[] = [];
  const codeSubstr: FastpathOption[] = [];
  const descSubstr: FastpathOption[] = [];
  for (const opt of options) {
    const code = opt.code.toUpperCase();
    const desc = (opt.description ?? '').toUpperCase();
    if (code.startsWith(query)) codePrefix.push(opt);
    else if (code.includes(query)) codeSubstr.push(opt);
    else if (desc.includes(query)) descSubstr.push(opt);
  }
  return [...codePrefix, ...codeSubstr, ...descSubstr];
}
