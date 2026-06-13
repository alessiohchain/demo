import { useEffect } from 'react';
import { createBrowserRouter, Outlet } from 'react-router-dom';
import { useAuth } from '@/app/auth/AuthProvider';
import { beginPlatformLogin } from '@/app/auth/platformSso';
import AuthCallback from '@/pages/AuthCallback';
import { AppShell } from '@/shell/AppShell';
import { Landing } from '@/shell/Landing';
import { DynamicScreen } from '@alessiohchain/csnx-engine/screen/DynamicScreen';

/**
 * Unauthenticated = straight into the OIDC code flow against the central
 * IdP (no module login page any more). While the IdP session cookie is
 * alive the round-trip is silent; otherwise the styled central login
 * page appears. The requested deep link rides along as returnTo.
 */
function RequireAuth() {
  const { isAuthenticated } = useAuth();
  useEffect(() => {
    if (!isAuthenticated) {
      void beginPlatformLogin(window.location.pathname + window.location.search);
    }
  }, [isAuthenticated]);
  if (!isAuthenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-sm text-muted-foreground">Redirecting to sign-in…</p>
      </div>
    );
  }
  return <Outlet />;
}

export const router = createBrowserRouter([
  // OIDC redirect target — public; it authenticates by exchanging the
  // one-time code it arrives with.
  { path: '/auth/callback', element: <AuthCallback /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppShell />,
        children: [
          { index: true, element: <Landing /> },
          { path: ':fastpath', element: <DynamicScreen /> },
          { path: ':fastpath/*', element: <DynamicScreen /> },
        ],
      },
    ],
  },
]);
