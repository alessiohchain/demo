import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { ChevronDown, Check } from 'lucide-react';
import { toast } from 'sonner';
import { useAuth } from '@/app/auth/AuthProvider';
import { cn } from '@/lib/utils';

/**
 * CSNX-13939 — header dropdown that lets the signed-in user change their
 * working facility / warehouse. Mirrors the GWT
 * {@code WAREHOUSE_CHANGE} → {@code resetApplication} → {@code reloadLookupData}
 * sequence:
 *
 * <ol>
 *   <li>POST {@code /api/auth/switch} with the new facility / warehouse.
 *       Server rewrites the in-Ignite session record and returns fresh
 *       lookup data for the new context.</li>
 *   <li>Update {@link useAuth} state (user + lookupData) so consumers see
 *       the new context immediately.</li>
 *   <li>Invalidate every TanStack-Query cache so any open screen refetches
 *       in the new context (metadata is per-warehouse, grid-search results
 *       are per-warehouse, lookup-data version bumps).</li>
 *   <li>Navigate to the landing page so any in-flight screen unmounts and
 *       its {@code ScreenProvider} state (selectedRows, formValues,
 *       editedRows, masterDetail) is dropped — same outcome as GWT's
 *       {@code resetApplication} closing every open tab.</li>
 * </ol>
 */
export function WarehouseSwitcher() {
  const { user, lookupData, switchFacilityWarehouse } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const wrapperRef = useRef<HTMLDivElement | null>(null);

  // Available F/W list comes from the WarehouseFacility VVD bundled at
  // login (and refreshed via /api/lookup). Keys are space-prefixed
  // "FACL/WHSE" strings — useLookup trims them, so we read straight from
  // lookupData here (this component sits next to AuthProvider and the
  // raw map is the simplest source).
  const options = useMemo(() => {
    const raw = lookupData['WarehouseFacility'] ?? {};
    return Object.entries(raw).map(([code, label]) => ({ code: code.trim(), label }));
  }, [lookupData]);

  const currentValue = user
    ? `${user.defaultFacility ?? ''}/${user.defaultWarehouse ?? ''}`
    : '';

  useEffect(() => {
    if (!open) return;
    function onDocMouseDown(e: MouseEvent) {
      const el = wrapperRef.current;
      if (el && !el.contains(e.target as Node)) setOpen(false);
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false);
    }
    document.addEventListener('mousedown', onDocMouseDown);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDocMouseDown);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  if (!user) return null;
  if (options.length <= 1) {
    // Only one F/W available — nothing to switch to. Render the static
    // text instead of a useless picker.
    return (
      <span className="text-sm text-muted-foreground">
        {user.companyCode} / {user.defaultFacility}/{user.defaultWarehouse}
      </span>
    );
  }

  async function pick(code: string) {
    if (busy) return;
    if (code === currentValue) {
      setOpen(false);
      return;
    }
    const [facility, warehouse] = code.split('/');
    if (!facility || !warehouse) {
      toast.error('Invalid facility/warehouse selection.');
      return;
    }
    setBusy(true);
    try {
      // 1. Navigate to the landing page FIRST so any open fastpath unmounts
      //    immediately. This drops the screen's {@code ScreenProvider} state
      //    (selectedRows, formValues, editedRows, masterDetail) AND tears
      //    down its TanStack-Query subscribers — so the {@code invalidateQueries}
      //    below can't trigger a stale-context refetch against the old screen
      //    mid-switch (which would render with the new F/W's lookup data
      //    against the old screen's metadata, briefly mis-mapping VVD codes).
      navigate('/', { replace: true });
      // 2. Swap the auth context to the new F/W. Server rewrites the Ignite
      //    session record and returns fresh lookup data.
      await switchFacilityWarehouse(facility, warehouse);
      // 3. Drop every cached query — metadata, grid-search, form-load are
      //    all facility/warehouse-scoped on the server side. By now the
      //    landing page is mounted and has no active queries, so this is
      //    cache cleanup with no immediate refetches.
      queryClient.removeQueries();
      toast.success(`Switched to ${facility}/${warehouse}`);
    } catch {
      // axios interceptor surfaced the structured error
    } finally {
      setBusy(false);
      setOpen(false);
    }
  }

  return (
    <div ref={wrapperRef} className="relative">
      <button
        type="button"
        disabled={busy}
        onClick={() => setOpen((v) => !v)}
        aria-haspopup="listbox"
        aria-expanded={open}
        className="inline-flex items-center gap-1 rounded-md border bg-background px-3 h-8 text-sm transition-colors hover:border-primary hover:bg-primary/5 hover:text-primary disabled:opacity-50"
      >
        <span className="text-muted-foreground">{user.companyCode} /</span>
        <span className="font-medium tabular-nums">{currentValue}</span>
        <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" aria-hidden />
      </button>
      {open && (
        <ul
          role="listbox"
          className="absolute right-0 top-full mt-1 z-30 max-h-72 min-w-56 overflow-y-auto rounded-md border bg-popover p-1 shadow-md text-sm"
        >
          {options.map((o) => {
            const selected = o.code === currentValue;
            return (
              <li key={o.code}>
                <button
                  type="button"
                  role="option"
                  aria-selected={selected}
                  onClick={() => pick(o.code)}
                  className={cn(
                    'flex w-full items-center justify-between gap-3 rounded-sm px-2 py-1.5 transition-colors hover:bg-primary/10 hover:text-primary',
                    selected && 'bg-primary/10 text-primary font-medium',
                  )}
                >
                  <span className="truncate">{o.label}</span>
                  <span className="flex items-center gap-2">
                    <span className="text-xs tabular-nums text-muted-foreground">{o.code}</span>
                    {selected && <Check className="h-3.5 w-3.5" aria-hidden />}
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
