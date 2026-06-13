import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/app/auth/AuthProvider';
import { completePlatformLogin, consumeReturnTo } from '@/app/auth/platformSso';

/**
 * OIDC redirect target (/auth/callback). Exchanges the one-time code for
 * tokens, bootstraps the engine session, and lands on the home screen.
 * On any failure the user is bounced back to /login with the message.
 */
export default function AuthCallback() {
  const navigate = useNavigate();
  const { loginWithPlatform } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const startedRef = useRef(false);

  useEffect(() => {
    // React 18 StrictMode double-invokes effects — the code is single-use,
    // so guard against a second exchange attempt.
    if (startedRef.current) return;
    startedRef.current = true;
    const params = new URLSearchParams(window.location.search);
    const code = params.get('code');
    const state = params.get('state');
    const oidcError = params.get('error');
    if (oidcError) {
      setError(params.get('error_description') ?? oidcError);
      return;
    }
    if (!code || !state) {
      setError('Missing sign-in response parameters.');
      return;
    }
    (async () => {
      try {
        const tokens = await completePlatformLogin(code, state);
        await loginWithPlatform(tokens);
        // Restore the deep link the user originally requested.
        navigate(consumeReturnTo(), { replace: true });
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Sign-in failed.');
      }
    })();
  }, [loginWithPlatform, navigate]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 p-6">
      {error ? (
        <>
          <p className="text-destructive text-sm">{error}</p>
          <button
            type="button"
            className="text-sm underline"
            onClick={() => window.location.assign('/')}
          >
            Try again
          </button>
        </>
      ) : (
        <p className="text-muted-foreground text-sm">Completing sign-in…</p>
      )}
    </div>
  );
}
