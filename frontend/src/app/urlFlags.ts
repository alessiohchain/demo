/**
 * CSNX-13939 — boot-time URL flags, mirroring {@code Csnx.java#load()}:
 *
 * <ul>
 *   <li>{@code ?DEVELOPMENT} — turn on the dev tooltip overlay so every
 *       field, button, and grid header shows its metadata on hover (matches
 *       {@code ApplicationHelper.setDevelopment(true)} →
 *       {@code fieldMetadata.setTooltip(metadata.toString())}).</li>
 *   <li>{@code ?CLEAR_CACHE} — fire a server cache flush before the shell
 *       loads (matches {@code cacheService.clearAll()}).</li>
 *   <li>{@code ?CACHE_UI=false} — disable client-side metadata / lookup
 *       caching so every screen rebuilds from the server (matches
 *       {@code ComponentMappings.CACHE_VIEWS = false}).</li>
 * </ul>
 *
 * <p>Read once at module load — same lifecycle as the GWT version. Reload
 * the tab to change a flag.
 */

// Case-insensitive flag lookup so {@code ?development=true},
// {@code ?DEVELOPMENT}, and {@code ?Development} all work. CSnx uses
// uppercase but it's friendlier not to make the user remember casing.
function findFlag(name: string): string | null {
  const target = name.toLowerCase();
  for (const [k, v] of new URLSearchParams(window.location.search).entries()) {
    if (k.toLowerCase() === target) return v;
  }
  return null;
}

/** True unless the flag value parses as a falsy literal ({@code "false"} /
 *  {@code "0"} / {@code "no"}). Bare presence ({@code ?FOO}) → true.
 *  Absent flag → falls back to {@code defaultWhenAbsent}. */
function flagIsTruthy(name: string, defaultWhenAbsent: boolean): boolean {
  const v = findFlag(name);
  if (v === null) return defaultWhenAbsent;
  if (v === '') return true; // bare presence: ?DEVELOPMENT
  const lc = v.toLowerCase();
  return lc !== 'false' && lc !== '0' && lc !== 'no';
}

/** Hover-tooltip-shows-metadata mode. Matches CSnx's
 *  {@code ApplicationHelper.setDevelopment(true)} branches. Truthy when
 *  any of: {@code ?DEVELOPMENT}, {@code ?DEVELOPMENT=true},
 *  {@code ?development=1}. False when {@code ?DEVELOPMENT=false}. */
export const IS_DEVELOPMENT: boolean = flagIsTruthy('DEVELOPMENT', false);

/** Whether the shell should call the backend cache-clear endpoint at
 *  startup. Truthy by mere presence (matches CSnx). */
export const SHOULD_CLEAR_CACHE: boolean = flagIsTruthy('CLEAR_CACHE', false);

/** Whether client-side caches (TanStack Query for metadata / lookups) are
 *  enabled. Default true; explicitly disabled by {@code ?CACHE_UI=false}.
 *  Bare {@code ?CACHE_UI} keeps caching on (matches the CSnx semantics
 *  where only the literal "false" turns it off). */
export const CACHE_UI_ENABLED: boolean = (() => {
  const v = findFlag('CACHE_UI');
  if (v == null) return true;
  return v.toLowerCase() !== 'false';
})();

/** Sort scope for grid columns. {@code true} (default) means the grid
 *  sorts the rows it has loaded in memory client-side — fast, no
 *  roundtrip, but only re-orders the current page. {@code false} (set by
 *  {@code ?CLIENT_SORT=false}) sends the sort field+direction to the
 *  server's {@code cmd_search} so each Next/Prev fetches a sorted batch
 *  ordered by the underlying SQL. Server-side is correct when there are
 *  more rows than fit in one page; client-side is responsive but only
 *  sorts the visible batch. */
export const CLIENT_SORT_ENABLED: boolean = flagIsTruthy('CLIENT_SORT', true);

// Optional: surface the resolved flags on window for quick console
// inspection during development. Tree-shaken in production builds via
// {@code import.meta.env.DEV}.
if (import.meta.env.DEV) {
  (window as unknown as { __csnxFlags?: object }).__csnxFlags = {
    IS_DEVELOPMENT,
    SHOULD_CLEAR_CACHE,
    CACHE_UI_ENABLED,
  };
}
