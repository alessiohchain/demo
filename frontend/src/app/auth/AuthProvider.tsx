import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type PropsWithChildren,
} from 'react';
import { api, setAccessToken, setUnauthorizedHandler } from '@/app/api/client';
import {
  beginPlatformLogin,
  clearPlatformSession,
  globalLogoutUrl,
  platformIssuer,
  type PlatformTokens,
} from '@/app/auth/platformSso';
import { normalizeLookupMap } from '@/lib/lookup';
import { clearCacheVersion, setCacheVersion } from '@alessiohchain/csnx-engine/process/cacheVersionStore';
import { clearWorkflowStateStore } from '@alessiohchain/csnx-engine/process/workflowStateStore';
import { clearUIModelStore } from '@alessiohchain/csnx-engine/process/uiModelStore';
import { clearParentUpdates } from '@alessiohchain/csnx-engine/process/parentRowsStore';
import { clearGridSnapshots } from '@alessiohchain/csnx-engine/process/parentGridStore';
import { clearBreadcrumbs } from '@alessiohchain/csnx-engine/breadcrumbs/breadcrumbStore';
import { clearFilterCriteria } from '@alessiohchain/csnx-engine/filter/filterCriteriaStore';
import { clearSearchCriteria } from '@alessiohchain/csnx-engine/filter/searchCriteriaStore';
import { clearAllEntityDialogSnapshots } from '@alessiohchain/csnx-engine/dialog/entityDialogStore';

export interface UserInfo {
  username: string;
  displayName?: string;
  companyCode: string;
  defaultWarehouse?: string;
  defaultFacility?: string;
  language?: string;
  roles?: string[];
}

export interface MenuItem {
  code: string;
  description: string;
  fastpath?: string;
  workflow?: string;
  children?: MenuItem[];
}

export type LookupData = Record<string, Record<string, string>>;

/** Flat fastpath-code -> workflow map, populated at login from the
 *  hierarchical menu plus the remainingFastpath list so direct fastpath
 *  input resolves even when the code isn't in the visible menu tree. */
export type FastpathMap = Record<string, string>;

export interface PasswordSettings {
  /** When true, password must contain at least one letter AND one digit. */
  alphaNumeric: boolean;
  /** Minimum password length. {@code 0} means no minimum. */
  minLength: number;
  /** When true, password must contain at least one upper-case AND one
   *  lower-case letter. */
  mixedCase: boolean;
  /** When true, password must contain at least one non-alphanumeric character. */
  specialCharacters: boolean;
}

/**
 * Per-module feature flags + landing/branding from the server bundle (the
 * platform's module_config row via MCFG). The shell hides features whose flag
 * is false (e.g. the AssistantBar when {@code smartNavigation} is false).
 */
export interface Features {
  smartNavigation: boolean;
  dashboard: boolean;
  smartReports: boolean;
  smartCapture: boolean;
  scheduling: boolean;
  defaultFastpath?: string;
  defaultMenu?: string;
  tileIcon?: string;
  tileColor?: string;
  /** Whether the header facility/warehouse chooser is shown (default true). */
  fwChooser?: boolean;
}

export interface LoginBundle {
  accessToken: string;
  user: UserInfo;
  menu: MenuItem[];
  fastpaths?: FastpathMap;
  lookupData: LookupData;
  lookupVersion: number;
  /** Per-module feature flags + landing/branding (null pre-login). */
  features?: Features;
  /** Optional help link; rendered in the shell only when present. */
  helpUrl?: string;
  /** Server-stamped metadata version. Paired with {@link lookupVersion} in
   *  the {@code cachVersion} round-trip — a bumped {@code metadataVersion}
   *  signals the cached SWET metadata is stale. */
  metadataVersion?: number;
  /** Free-form server build/version string (mirrors what the legacy GWT
   *  shell rendered via {@code HeaderDTO.setVersionInfo}). */
  versionInfo?: string;
  /** Password rules (alphaNumeric / minLength / mixedCase / specialCharacters)
   *  the client uses to validate password fields before submit. Mirrors
   *  GWT's {@code AppInitData.passwordSettings}. */
  passwordSettings?: PasswordSettings;
}

/**
 * Result of a login / change-password call. The bundle is returned to the
 * caller so it can route based on {@code changePasswordRequired} (forced
 * change-password flow) without each call site having to refetch
 * AuthProvider state.
 */
export interface LoginResult {
  /** When true, the server requires a password change before access is
   *  granted. The user/menu/lookup fields will be unset; the caller should
   *  navigate to the change-password screen. */
  changePasswordRequired: boolean;
  /** The full bundle as received — kept for callers that want to inspect
   *  warning / confirm messages. */
  bundle: LoginBundle;
}

interface AuthContextValue {
  user: UserInfo | null;
  menu: MenuItem[];
  fastpaths: FastpathMap;
  lookupData: LookupData;
  lookupVersion: number;
  versionInfo: string | null;
  passwordSettings: PasswordSettings | null;
  /** Per-module feature flags + landing/branding; null until bootstrap. */
  features: Features | null;
  /** Optional help link; null until bootstrap. */
  helpUrl: string | null;
  isAuthenticated: boolean;
  /** Complete the platform-SSO sign-in: adopt the platform-issued access
   *  token, then bootstrap the engine bundle from /api/session/bootstrap. */
  loginWithPlatform: (tokens: PlatformTokens) => Promise<void>;
  logout: () => Promise<void>;
  /** Switch the user's working facility/warehouse — replaces the in-memory
   *  user + lookup data with the response from {@code /api/auth/switch}.
   *  Callers should also invalidate React-Query caches and reset
   *  per-screen state (the GWT analogue is {@code resetApplication}). */
  switchFacilityWarehouse: (facility: string, warehouse: string) => Promise<void>;
}

/** Provider mention of {@link LoginBundle} — exported separately so
 *  Login.tsx can read the changePasswordRequired flag without re-typing
 *  the shape. */
type LoginBundleResponse = LoginBundle & { changePasswordRequired?: boolean };

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<UserInfo | null>(null);
  const [menu, setMenu] = useState<MenuItem[]>([]);
  const [fastpaths, setFastpaths] = useState<FastpathMap>({});
  const [lookupData, setLookupData] = useState<LookupData>({});
  const [lookupVersion, setLookupVersion] = useState(0);
  const [versionInfo, setVersionInfo] = useState<string | null>(null);
  const [passwordSettings, setPasswordSettings] = useState<PasswordSettings | null>(null);
  const [features, setFeatures] = useState<Features | null>(null);
  const [helpUrl, setHelpUrl] = useState<string | null>(null);

  const clear = useCallback(() => {
    clearPlatformSession();
    setUser(null);
    setMenu([]);
    setFastpaths({});
    setLookupData({});
    setLookupVersion(0);
    setVersionInfo(null);
    setPasswordSettings(null);
    setFeatures(null);
    setHelpUrl(null);
    setAccessToken(null);
    clearCacheVersion();
    clearWorkflowStateStore();
    clearUIModelStore();
    clearParentUpdates();
    clearGridSnapshots();
    clearAllEntityDialogSnapshots();
    clearBreadcrumbs();
    clearFilterCriteria();
    clearSearchCriteria();
    // Grid layout preferences are intentionally NOT wiped on logout —
    // the user's column order / visibility / sort survive the
    // session boundary so they get the same layout when they sign back
    // in. Keys include the username to keep multi-user shared browsers
    // honest.
  }, []);

  // 401 anywhere = the 15-min access token expired (the IdP issues no
  // refresh tokens to public clients). Clear local state and silently
  // re-run the OIDC code flow — instant while the IdP session cookie is
  // alive; otherwise the user lands on the central login page. The
  // redirecting flag stops a burst of parallel 401s from stacking
  // multiple redirects.
  const redirectingRef = useRef(false);
  useEffect(() => {
    setUnauthorizedHandler(() => {
      if (redirectingRef.current) return;
      redirectingRef.current = true;
      clear();
      void beginPlatformLogin();
    });
  }, [clear]);

  // Apply a populated login bundle to local state. Shared by login and
  // change-password since both responses have the same shape.
  const applyBundle = useCallback((data: LoginBundleResponse) => {
    // Always pick up password settings — they're available even on a
    // changePasswordRequired response so the change-password screen
    // can validate against them.
    setPasswordSettings(data.passwordSettings ?? null);
    // When the response requires change-password, the user/token fields
    // are absent. Don't promote to "authenticated" — leave AuthProvider
    // in its current state so the change-password screen can run
    // unauthenticated.
    if (data.accessToken && data.user) {
      setAccessToken(data.accessToken);
      setUser(data.user);
      setMenu(data.menu);
      setFastpaths(data.fastpaths ?? {});
      setLookupData(normalizeLookupMap(data.lookupData));
      setLookupVersion(data.lookupVersion);
      setVersionInfo(data.versionInfo ?? null);
      setFeatures(data.features ?? null);
      setHelpUrl(data.helpUrl ?? null);
      // Seed the version pair so every subsequent /api/process round-trip
      // ships {@code cachVersion} for drift detection.
      setCacheVersion({
        metadata: data.metadataVersion,
        lookupData: data.lookupVersion,
      });
    }
  }, []);

  const loginWithPlatform = useCallback<AuthContextValue['loginWithPlatform']>(
    async (tokens) => {
      setAccessToken(tokens.accessToken);
      const { data } = await api.get<LoginBundleResponse>('/api/session/bootstrap');
      applyBundle({ ...data, accessToken: tokens.accessToken });
    },
    [applyBundle],
  );



  const logout = useCallback(async () => {
    // Navigate FIRST, without touching React state: clear()-ing auth
    // state here re-renders RequireAuth, whose effect fires its own
    // beginPlatformLogin() redirect that races (and can beat) the
    // end-session navigation — leaving the user silently re-signed-in.
    // The full-page navigation wipes all in-memory state anyway; only
    // the sessionStorage id_token needs explicit clearing.
    // Without an id_token, the IdP's logout endpoint still works — it
    // just shows its confirmation page instead of redirecting back.
    const endSession = globalLogoutUrl();
    clearPlatformSession();
    window.location.assign(endSession ?? `${platformIssuer()}/connect/logout`);
  }, []);

  const switchFacilityWarehouse = useCallback(
    async (facility: string, warehouse: string) => {
      const { data } = await api.post<{
        user: UserInfo;
        lookupData: LookupData;
        lookupVersion: number;
      }>('/api/session/switch', { facility, warehouse });
      setUser(data.user);
      setLookupData(normalizeLookupMap(data.lookupData));
      setLookupVersion(data.lookupVersion);
      // Refresh the cached lookupData version so the next /api/process
      // round-trip carries the post-switch number — otherwise the server
      // would keep reporting drift on every call.
      setCacheVersion({ lookupData: data.lookupVersion });
    },
    [],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      menu,
      fastpaths,
      lookupData,
      lookupVersion,
      versionInfo,
      passwordSettings,
      features,
      helpUrl,
      isAuthenticated: !!user,
      loginWithPlatform,
      logout,
      switchFacilityWarehouse,
    }),
    [user, menu, fastpaths, lookupData, lookupVersion, versionInfo, passwordSettings, features, helpUrl, loginWithPlatform, logout, switchFacilityWarehouse],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
