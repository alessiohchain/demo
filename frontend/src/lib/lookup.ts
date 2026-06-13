/**
 * CSnx lookup data sometimes ships with whitespace-padded keys (a legacy
 * convention used for alignment in the GWT UI). The keys are also the values
 * the form submits — `"WAREHOUSE": { " 00": "Default" }` would surface a
 * leading space in the request body and break server-side equality checks
 * (`UserManager.toUpperCase()` doesn't trim, see CSNX-13939 login bug).
 *
 * Normalise once at ingestion so every consumer (login screen, FieldRenderer
 * select, MultiSelect, autocomplete, master-detail filters) gets clean values.
 *
 * Trimming behaviour:
 *  - Keys: leading + trailing whitespace stripped.
 *  - Values (display labels): whitespace preserved — labels intentionally use
 *    leading spaces for indentation/grouping in some lookups.
 *  - Duplicate keys after trim collapse to the last entry; this matches what
 *    the existing GWT engine does (it puts space-padded entries into a
 *    LinkedHashMap that overwrites).
 */
export type LookupMap = Record<string, Record<string, string>>;

/** Trim keys (the submit values) of every inner map. Returns a new object. */
export function normalizeLookupMap(data: LookupMap | undefined): LookupMap {
  if (!data) return {};
  const out: LookupMap = {};
  for (const [lookupKey, entries] of Object.entries(data)) {
    const trimmed: Record<string, string> = {};
    for (const [code, label] of Object.entries(entries ?? {})) {
      trimmed[code.trim()] = label;
    }
    out[lookupKey] = trimmed;
  }
  return out;
}
