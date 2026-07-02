// Runtime environment for the demo SPA. This placeholder (empty = fall back to
// the bundle's localhost defaults) is what `npm run dev` serves; the container
// entrypoint (docker-entrypoint.sh) overwrites this file at startup from the
// PLATFORM_ISSUER / PORTAL_URL env vars, so one image serves every environment.
window.__PLATFORM_ENV__ = { platformIssuer: '', portalUrl: '' };
