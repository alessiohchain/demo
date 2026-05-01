import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  authApi,
  registerRefreshHook,
  setAccessToken,
  type LoginPayload,
  type RegisterPayload,
} from "./api";

type AuthState = {
  isAuthenticated: boolean;
  isInitializing: boolean;
};

type AuthContextValue = AuthState & {
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({
    isAuthenticated: false,
    isInitializing: true,
  });

  const refresh = useCallback(async (): Promise<string | null> => {
    try {
      const res = await authApi.refresh();
      setAccessToken(res.accessToken);
      setState({ isAuthenticated: true, isInitializing: false });
      return res.accessToken;
    } catch {
      setAccessToken(null);
      setState({ isAuthenticated: false, isInitializing: false });
      return null;
    }
  }, []);

  useEffect(() => {
    registerRefreshHook(refresh);
    void refresh();
  }, [refresh]);

  const login = useCallback(async (payload: LoginPayload) => {
    const res = await authApi.login(payload);
    setAccessToken(res.accessToken);
    setState({ isAuthenticated: true, isInitializing: false });
  }, []);

  const register = useCallback(async (payload: RegisterPayload) => {
    await authApi.register(payload);
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      setAccessToken(null);
      setState({ isAuthenticated: false, isInitializing: false });
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ ...state, login, register, logout }),
    [state, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
