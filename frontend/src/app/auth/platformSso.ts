/**
 * OIDC authorization-code + PKCE client against the platform IdP — the
 * ONLY sign-in path (module-local login retired; the IdP serves the
 * styled login page). No tokens ever appear in URLs:
 *
 *  - the redirect carries only the one-time authorization code;
 *  - the PKCE verifier/state/returnTo live in sessionStorage only between
 *    the redirect out and the callback;
 *  - the access token lives in module-scope memory. Expiry or reload
 *    silently re-runs the code flow — instant while the IdP session
 *    cookie is alive. (The IdP issues no refresh tokens to public
 *    clients.)
 *  - the id_token (identity claims only, no API access) is kept in
 *    sessionStorage so OIDC RP-initiated logout still works after a
 *    reload.
 */

declare global {
  interface Window {
    /** Rendered at container start from PLATFORM_ISSUER / PORTAL_URL (see public/config.js). */
    __PLATFORM_ENV__?: { platformIssuer?: string; portalUrl?: string };
  }
}

// Resolution order: runtime config (one image, per-environment env vars) →
// build-time Vite var (dev-server override) → local-compose default.
const ISSUER: string =
  window.__PLATFORM_ENV__?.platformIssuer ||
  (import.meta.env.VITE_PLATFORM_ISSUER as string | undefined) ||
  'http://localhost:8090';
const CLIENT_ID = 'demo-module';

/**
 * The central portal origin (no trailing slash). Sign-out lands here so the
 * next sign-in is routed by the portal — straight into a single-module user's
 * one module, or a chooser for multi-module users. Also the fallback target
 * when a user reaches a module they cannot access (see {@code moduleClaim.ts}).
 * Must match a registered post-logout-redirect-uri on this module's IdP client.
 */
export const PORTAL_URL: string = (
  window.__PLATFORM_ENV__?.portalUrl ||
  (import.meta.env.VITE_PORTAL_URL as string | undefined) ||
  'http://localhost:8091'
).replace(/\/+$/, '');
const VERIFIER_KEY = 'platform_pkce_verifier';
const STATE_KEY = 'platform_pkce_state';
const RETURN_TO_KEY = 'platform_return_to';
const ID_TOKEN_KEY = 'platform_id_token';

export function platformIssuer(): string {
  return ISSUER;
}

export function clearPlatformSession(): void {
  sessionStorage.removeItem(ID_TOKEN_KEY);
}

/**
 * OIDC RP-initiated logout URL — ends the IdP session (the cross-module
 * SSO session) and lands on the central portal, which routes the next
 * sign-in (a chooser for multi-module users, straight-through for
 * single-module). Null when no id_token survived (e.g. storage cleared):
 * callers fall back to a plain IdP-login redirect.
 */
export function globalLogoutUrl(): string | null {
  const idToken = sessionStorage.getItem(ID_TOKEN_KEY);
  if (!idToken) return null;
  const params = new URLSearchParams({
    id_token_hint: idToken,
    post_logout_redirect_uri: `${PORTAL_URL}/`,
  });
  return `${ISSUER}/connect/logout?${params.toString()}`;
}

function randomUrlSafe(bytes: number): string {
  const buf = new Uint8Array(bytes);
  crypto.getRandomValues(buf);
  return base64Url(buf);
}

function base64Url(bytes: Uint8Array): string {
  let s = '';
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

async function s256(verifier: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier));
  return base64Url(new Uint8Array(digest));
}

function redirectUri(): string {
  return `${window.location.origin}/auth/callback`;
}

/**
 * Kick off the code flow — navigates the browser to the IdP. `returnTo`
 * (default: the current in-app location) is restored by the callback so
 * deep links survive the round-trip.
 */
export async function beginPlatformLogin(returnTo?: string): Promise<void> {
  const verifier = randomUrlSafe(48);
  const state = randomUrlSafe(16);
  sessionStorage.setItem(VERIFIER_KEY, verifier);
  sessionStorage.setItem(STATE_KEY, state);
  sessionStorage.setItem(
    RETURN_TO_KEY,
    returnTo ?? window.location.pathname + window.location.search,
  );
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: CLIENT_ID,
    redirect_uri: redirectUri(),
    scope: 'openid profile',
    state,
    code_challenge: await s256(verifier),
    code_challenge_method: 'S256',
  });
  window.location.assign(`${ISSUER}/oauth2/authorize?${params.toString()}`);
}

/** The deep link stored by {@link beginPlatformLogin}; consumed once. */
export function consumeReturnTo(): string {
  const stored = sessionStorage.getItem(RETURN_TO_KEY);
  sessionStorage.removeItem(RETURN_TO_KEY);
  return stored && stored.startsWith('/') && !stored.startsWith('/auth/') ? stored : '/';
}

export interface PlatformTokens {
  accessToken: string;
  expiresInSec: number;
}

/** Callback half of the flow: validate state, redeem the code. */
export async function completePlatformLogin(code: string, state: string): Promise<PlatformTokens> {
  const expectedState = sessionStorage.getItem(STATE_KEY);
  const verifier = sessionStorage.getItem(VERIFIER_KEY);
  sessionStorage.removeItem(STATE_KEY);
  sessionStorage.removeItem(VERIFIER_KEY);
  if (!verifier || !expectedState || expectedState !== state) {
    throw new Error('Sign-in state mismatch — please try again.');
  }
  const response = await fetch(`${ISSUER}/oauth2/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      code,
      redirect_uri: redirectUri(),
      client_id: CLIENT_ID,
      code_verifier: verifier,
    }).toString(),
  });
  if (!response.ok) {
    throw new Error(`Token request failed (${response.status})`);
  }
  const body = (await response.json()) as {
    access_token: string;
    id_token?: string;
    expires_in: number;
  };
  if (body.id_token) {
    sessionStorage.setItem(ID_TOKEN_KEY, body.id_token);
  }
  return { accessToken: body.access_token, expiresInSec: body.expires_in };
}

/**
 * Re-confirm a credential pair against the IdP — backs the engine's
 * re-authentication dialogs (ReauthDialog / MessagePromptDialog). Plain
 * fetch (cross-origin to the IdP; CORS allows module origins). Resolves
 * to the server-confirmed username; rejects with a displayable message.
 */
export async function verifyCredentials(
  companyCode: string,
  username: string,
  password: string,
): Promise<string> {
  const response = await fetch(`${ISSUER}/api/auth/verify`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ companyCode, username, password }),
  });
  if (!response.ok) {
    throw new Error(
      response.status === 401 ? 'Verification failed.' : `Verification failed (${response.status}).`,
    );
  }
  const body = (await response.json()) as { username?: string };
  return body.username ?? username;
}
