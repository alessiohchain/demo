import axios, { AxiosError, type AxiosInstance } from 'axios';
import { toast } from 'sonner';
import { errorBus } from '@/app/error/errorBus';
import { getCacheVersion } from '@alessiohchain/csnx-engine/process/cacheVersionStore';
import { getWorkflowModels, getWorkflowOverride } from '@alessiohchain/csnx-engine/process/workflowStateStore';
import { getPreviousFastpath } from '@alessiohchain/csnx-engine/process/workflowTracker';
import { stripInlineErrorMarkup } from '@alessiohchain/csnx-engine/process/errorMessage';

let accessToken: string | null = null;
let onUnauthorized: (() => void) | null = null;

export const api: AxiosInstance = axios.create({
  baseURL: '/',
  headers: { 'Content-Type': 'application/json' },
});

export function setAccessToken(token: string | null) {
  accessToken = token;
}

/** The in-memory bearer token (a platform-issued JWT), or null pre-login.
 *  Read by the module-list claim decoder for the module switcher. */
export function getAccessToken(): string | null {
  return accessToken;
}

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler;
}

// Lightweight global network-activity counter. Every axios request bumps
// it; every response (success OR error) decrements it. Subscribers (the
// NetworkActivityBar in the shell) re-render whenever the count crosses
// 0/non-zero so we get a unified loader for ALL HTTP traffic — covering
// raw axios calls (auto-refresh polling) as well as TanStack-driven ones.
let inflight = 0;
const inflightSubs = new Set<(n: number) => void>();
function notifyInflight() {
  for (const fn of inflightSubs) fn(inflight);
}
export function getInflightCount(): number {
  return inflight;
}
export function subscribeInflight(fn: (n: number) => void): () => void {
  inflightSubs.add(fn);
  return () => {
    inflightSubs.delete(fn);
  };
}

/**
 * Force the inflight counter back to 0 and notify subscribers, so the
 * {@link NetworkActivityBar} stops animating immediately. Used when the
 * user starts a fresh screen flow (fastpath / menu click / landing
 * mount) — the prior screen's in-flight server call is no longer
 * relevant once they've navigated away, so the loader shouldn't keep
 * spinning while it resolves.
 *
 * <p>The clamp on each request's response decrement
 * ({@code Math.max(0, inflight - 1)}) keeps the counter honest if those
 * already-pending requests do eventually return: they decrement from
 * 0 → 0 rather than going negative. A NEW request started after the
 * reset increments from 0 → 1 and shows the bar again as normal.
 */
export function resetInflight(): void {
  if (inflight === 0) return;
  inflight = 0;
  notifyInflight();
}

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  // Attach the client's current cache versions to every {@code /api/process}
  // request so the server can detect drift. Mirrors GWT's
  // {@code DynamicContentController.handleRemoteCall} where every request
  // sets {@code processDataHolder.setCachVersion(ApplicationHelper.getCacheVersion())}.
  // Done here (interceptor) rather than per-call site so {@code useProcess},
  // {@code useGridSearch}, and {@code useFormLoad} all get it without
  // duplicating the wiring.
  if (config.url && config.url.startsWith('/api/process')) {
    const data = (config.data ?? {}) as Record<string, unknown>;
    let mutated: Record<string, unknown> | null = null;
    const version = getCacheVersion();
    if ((version.metadata != null || version.lookupData != null) && data.cachVersion == null) {
      mutated = { ...(mutated ?? data), cachVersion: version };
    }
    // {@code workflowOverride} sticky re-route — every request rewrites its
    // workflow to the override when one is set. Mirrors GWT's
    // {@code DynamicContentController.setupWorkflow}: if an override is
    // present it wins over whatever the metadata named, otherwise the
    // caller's value passes through.
    const override = getWorkflowOverride();
    if (override && data.workflow !== override) {
      mutated = { ...(mutated ?? data), workflow: override };
    }
    // {@code workflowModels} cross-call state — replay on every outbound
    // request so the activity sees its prior stash. Caller-supplied wins
    // (a test or a dialog can override the replay).
    const models = getWorkflowModels();
    if (Object.keys(models).length > 0 && data.workflowModels == null) {
      mutated = { ...(mutated ?? data), workflowModels: models };
    }
    // {@code previousFasthPath} — paired with {@code previousWorkflow} so
    // activities can distinguish "navigated in" vs. fastpath jump. Only
    // set when the caller didn't already.
    const prevFp = getPreviousFastpath();
    if (prevFp && data.previousFasthPath == null) {
      mutated = { ...(mutated ?? data), previousFasthPath: prevFp };
    }
    if (mutated) {
      config.data = mutated;
    }
  }
  inflight += 1;
  notifyInflight();
  return config;
});

api.interceptors.response.use(
  (response) => {
    inflight = Math.max(0, inflight - 1);
    notifyInflight();
    return response;
  },
  async (error: AxiosError<ProblemBody>) => {
    inflight = Math.max(0, inflight - 1);
    notifyInflight();
    const original = error.config;
    const status = error.response?.status;
    // Opt-out of the global error toast for best-effort/background calls
    // (e.g. assistant feedback) that must never surface a user-facing error.
    const silent = Boolean((original as RetryConfig & { silent?: boolean })?.silent);

    // 401: the IdP issues no refresh tokens — the unauthorized handler
    // silently re-runs the OIDC code flow (instant while the IdP session
    // cookie is alive). No retry here; the page redirect supersedes it.
    if (status === 401) {
      onUnauthorized?.();
      return Promise.reject(error);
    }

    // structured CSnx error: dispatch on `kind`
    const body = error.response?.data;
    if (silent) {
      // suppress all user-facing toasts for this request
    } else if (body && typeof body === 'object' && 'kind' in body) {
      switch (body.kind) {
        case 'business':
          toast.error(stripInlineErrorMarkup(body.detail ?? body.title ?? 'Error'));
          break;
        case 'multiple':
          errorBus.emit({
            title: body.title ?? 'Multiple errors',
            errors: body.errors ?? [],
          });
          break;
        case 'internal':
        default:
          toast.error(body.detail ?? 'Unexpected error — see logs');
          break;
      }
    } else if (status && status >= 400) {
      toast.error(friendlyHttpMessage(status));
    } else if (!error.response) {
      // Network failure / CORS / server unreachable — no response object.
      toast.error("Can't reach the server. Check your connection and try again.");
    }

    return Promise.reject(error);
  },
);

/** Map an HTTP status to a user-facing message. Keeps internals (codes,
 *  stack traces, "request failed") out of the toast — those go to the
 *  catalina log. */
function friendlyHttpMessage(status: number): string {
  if (status === 400) return "We couldn't process that request. Please check the values and try again.";
  if (status === 401) return 'Your session has expired. Please sign in again.';
  if (status === 403) return "You don't have permission to do that.";
  if (status === 404) return "What you're looking for couldn't be found.";
  if (status === 408 || status === 504) return 'The request timed out. Please try again.';
  if (status === 409) return 'That conflicts with another change. Refresh and try again.';
  if (status === 422) return "We couldn't accept that change. Please review and try again.";
  if (status === 429) return 'Too many requests right now. Please slow down and try again.';
  if (status >= 500) return 'Something went wrong on the server. Please try again shortly.';
  return 'Something went wrong. Please try again.';
}

type RetryConfig = { _retry?: boolean };

export interface ProblemError {
  field?: string;
  code?: string;
  message: string;
}

export interface ProblemBody {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  kind: 'business' | 'multiple' | 'internal' | 'validation';
  errors?: ProblemError[];
}
