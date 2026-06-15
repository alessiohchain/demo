import type { EngineModuleTile } from '@alessiohchain/csnx-engine';
import { getAccessToken } from '@/app/api/client';

/** This module's catalog code (matches platformschema.module.module_cd). */
export const MODULE_CODE = 'DEMO';

/** Decode a base64url JWT segment to its JSON payload. */
function decodeSegment(segment: string): unknown {
  const base64 = segment.replace(/-/g, '+').replace(/_/g, '/');
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
  return JSON.parse(decodeURIComponent(escape(window.atob(padded))));
}

/**
 * The user's launchable modules, read straight off the in-memory access
 * token's `modules` claim (stamped by the platform token customizer). Feeds
 * the engine's module switcher — no extra HTTP, no cross-origin call.
 */
export function readModulesClaim(): EngineModuleTile[] {
  const token = getAccessToken();
  if (!token) return [];
  try {
    const payload = decodeSegment(token.split('.')[1]) as { modules?: EngineModuleTile[] };
    return Array.isArray(payload.modules) ? payload.modules : [];
  } catch {
    return [];
  }
}
