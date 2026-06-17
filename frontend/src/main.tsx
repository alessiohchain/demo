import { StrictMode, useEffect, useMemo } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from 'react-router-dom';
import { router } from '@/app/router';
import { AuthProvider, useAuth } from '@/app/auth/AuthProvider';
import { api, resetInflight, getInflightCount, subscribeInflight } from '@/app/api/client';
import { verifyCredentials, platformIssuer } from '@/app/auth/platformSso';
import { readModulesClaim, moduleAccessRedirect, MODULE_CODE } from '@/app/auth/moduleClaim';
import { CLIENT_SORT_ENABLED, IS_DEVELOPMENT } from '@/app/urlFlags';
import { EngineRuntimeProvider, type EngineStatics } from '@alessiohchain/csnx-engine';
import { ThemeProvider } from '@/app/theme/ThemeProvider';
import { ThemedToaster } from '@/app/theme/ThemedToaster';
import { MultiErrorDialog } from '@/app/error/MultiErrorDialog';
import { CACHE_UI_ENABLED, SHOULD_CLEAR_CACHE } from '@/app/urlFlags';
import { clearCacheVersion } from '@alessiohchain/csnx-engine/process/cacheVersionStore';
import { clearGridSnapshots } from '@alessiohchain/csnx-engine/process/parentGridStore';
import { clearParentUpdates } from '@alessiohchain/csnx-engine/process/parentRowsStore';
import { clearUIModelStore } from '@alessiohchain/csnx-engine/process/uiModelStore';
import { clearWorkflowStateStore } from '@alessiohchain/csnx-engine/process/workflowStateStore';
import { clearBreadcrumbs } from '@alessiohchain/csnx-engine/breadcrumbs/breadcrumbStore';
import { clearFilterCriteria } from '@alessiohchain/csnx-engine/filter/filterCriteriaStore';
import { clearSearchCriteria } from '@alessiohchain/csnx-engine/filter/searchCriteriaStore';
import './index.css';

// {@code ?CLEAR_CACHE} — match {@code Csnx.java#load()}'s pre-login
// {@code cacheService.clearAll()}: flush BOTH halves of the cache pair.
//
//   1. Server side — POST /api/cache/clear-all, which delegates to
//      {@code CacheProcessor.clearAll()} (metadata + menu + lookup-data
//      Ignite caches). Endpoint is public (see applicationContext-rest.xml
//      exclude-mapping) so this works pre-login.
//   2. Client side — wipe every module-level store that survives the SPA
//      lifecycle: row-update buffer, parent-grid snapshot, workflow state,
//      UI-model stash, and the cachVersion pair we send back on every
//      /api/process call. {@code queryClient.clear()} below covers the
//      TanStack-Query response cache.
//
// Page reloads already reset module state on their own; the explicit
// clearing here covers the SPA-internal future where a button could fire
// the same routine without a full reload, and serves as documentation of
// what the flag actually wipes.
if (SHOULD_CLEAR_CACHE) {
  void fetch('/api/cache/clear-all', { method: 'POST' }).catch(() => undefined);
  clearCacheVersion();
  clearWorkflowStateStore();
  clearUIModelStore();
  clearParentUpdates();
  clearGridSnapshots();
  clearBreadcrumbs();
  clearFilterCriteria();
  clearSearchCriteria();
}

// {@code ?CACHE_UI=false} forces every query to re-fetch on mount —
// matches GWT's {@code ComponentMappings.CACHE_VIEWS = false} which
// disabled the {@code DynamicContentControllerHelper} per-workflow
// controller cache. With staleTime + gcTime at 0, TanStack Query treats
// every component mount as a fresh fetch, no in-memory reuse.
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
      refetchOnWindowFocus: false,
      staleTime: CACHE_UI_ENABLED ? undefined : 0,
      gcTime: CACHE_UI_ENABLED ? undefined : 0,
    },
  },
});

// {@code ?CLEAR_CACHE} — wipe the just-created TanStack-Query cache so
// the first metadata / lookup fetch after the server flush genuinely hits
// the wire. {@code .clear()} discards every cached query and observer.
if (SHOULD_CLEAR_CACHE) {
  queryClient.clear();
}

/**
 * Bridges {@link AuthProvider}'s {@code user.username} into
 * {@link ThemeProvider} so per-user theme preferences load from the
 * correct localStorage bucket once authenticated, and fall back to the
 * shared bucket on the login screen.
 */
// The engine's only seam to the app: HTTP client, flags, credential
// verify (statics) + the live session slice from AuthProvider. See
// engine/runtime/EngineRuntime.tsx.
const engineStatics: EngineStatics = {
  api,
  resetInflight,
  verifyCredentials,
  getInflightCount,
  subscribeInflight,
  flags: {
    cacheUi: CACHE_UI_ENABLED,
    clientSort: CLIENT_SORT_ENABLED,
    isDevelopment: IS_DEVELOPMENT,
  },
};

function ThemedShell() {
  const { user, menu, fastpaths, lookupData, logout, versionInfo, features, switchFacilityWarehouse } =
    useAuth();
  // The user's launchable modules ride the access-token claim; recompute when
  // the session identity changes (login / silent re-auth).
  const modules = useMemo(() => readModulesClaim(), [user]);
  // If the signed-in user has no access to THIS module (e.g. they signed in
  // here after switching identities), route them like a normal login: straight
  // into their one module, or the portal to choose when they have several.
  useEffect(() => {
    if (!user) return;
    const target = moduleAccessRedirect(modules);
    if (target) window.location.assign(target);
  }, [user, modules]);
  const changePasswordUrl = user
    ? `${platformIssuer()}/change-password?company=${encodeURIComponent(user.companyCode ?? '')}&user=${encodeURIComponent(user.username ?? '')}`
    : undefined;
  return (
    <EngineRuntimeProvider
      statics={engineStatics}
      session={{
        user,
        menu,
        fastpaths,
        lookupData,
        logout,
        versionInfo: versionInfo ?? undefined,
        features: features ?? undefined,
        changePasswordUrl,
        modules,
        currentModuleCode: MODULE_CODE,
        switchFacilityWarehouse,
      }}
    >
      <ThemeProvider username={user?.username}>
        <RouterProvider router={router} />
        <MultiErrorDialog />
        <ThemedToaster />
      </ThemeProvider>
    </EngineRuntimeProvider>
  );
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <ThemedShell />
      </AuthProvider>
    </QueryClientProvider>
  </StrictMode>,
);
