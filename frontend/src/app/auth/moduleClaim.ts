import type { EngineModuleTile } from '@alessiohchain/csnx-engine';
import { getAccessToken } from '@/app/api/client';
import { PORTAL_URL } from '@/app/auth/platformSso';

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

/**
 * Where to send an authenticated user given the modules they can reach:
 *  - has access to THIS module -> null (stay)
 *  - exactly one other module  -> that module's baseUrl (go straight in)
 *  - none, or several          -> the portal to choose
 * Mirrors the portal Landing's single-module auto-forward so someone who signs
 * in against a module they cannot use still lands somewhere sensible instead of
 * on an empty, menu-less shell.
 */
export function moduleAccessRedirect(modules: EngineModuleTile[]): string | null {
  if (modules.some((m) => m.cd === MODULE_CODE)) return null;
  if (modules.length === 1) return modules[0].baseUrl;
  return PORTAL_URL;
}
