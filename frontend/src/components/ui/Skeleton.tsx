import { cn } from '@/lib/utils';

/**
 * CSNX-13939 — pulsing rectangle for loading placeholders. Matches the
 * shadcn/ui skeleton primitive: a muted background tinted to the
 * current theme, animated with Tailwind's {@code animate-pulse}.
 *
 * <p>Use the composed {@link FormSkeleton} / {@link GridSkeleton} when
 * the surrounding metadata is in hand — they shape the skeleton to the
 * field / column count so the loading state has the same footprint as
 * the real content (no layout jump on first paint).
 */
export function Skeleton({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn('animate-pulse rounded-md bg-muted/70', className)}
      {...props}
    />
  );
}

/**
 * Form-shaped loading placeholder. Renders {@code fieldCount} stub rows
 * in a 3-column grid so the proportions match the real
 * {@link DynamicForm} (which lays fields out in 3 columns on desktop).
 * Caller passes the metadata's field count so the skeleton occupies
 * roughly the same vertical space as the real form once loaded.
 */
export function FormSkeleton({
  fieldCount = 12,
  className,
}: {
  fieldCount?: number;
  className?: string;
}) {
  return (
    <div className={cn('space-y-6', className)} aria-busy="true" aria-live="polite">
      <div className="grid grid-cols-1 gap-x-6 gap-y-5 md:grid-cols-3">
        {Array.from({ length: fieldCount }).map((_, i) => (
          <div key={i} className="space-y-2">
            <Skeleton className="h-3 w-24" />
            <Skeleton className="h-9 w-full" />
          </div>
        ))}
      </div>
    </div>
  );
}

/**
 * Table-shaped loading placeholder. Renders a header row + {@code rowCount}
 * body rows with {@code columnCount} bars per row. Caller passes the
 * grid's metadata column count so the skeleton matches the visible
 * shape — important on PUTR / RFTM where 10+ columns scroll
 * horizontally; a generic three-column skeleton would mislead.
 */
export function GridSkeleton({
  columnCount = 6,
  rowCount = 8,
  className,
}: {
  columnCount?: number;
  rowCount?: number;
  className?: string;
}) {
  return (
    <div
      className={cn('w-full overflow-hidden', className)}
      aria-busy="true"
      aria-live="polite"
    >
      <div className="border-b border-border bg-muted/30 px-3 py-2 flex gap-3">
        {Array.from({ length: columnCount }).map((_, i) => (
          <Skeleton key={i} className="h-4 flex-1 min-w-16" />
        ))}
      </div>
      <div className="divide-y divide-border">
        {Array.from({ length: rowCount }).map((_, r) => (
          <div key={r} className="px-3 py-3 flex gap-3 items-center">
            {Array.from({ length: columnCount }).map((_, c) => (
              <Skeleton key={c} className="h-4 flex-1 min-w-16" />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
