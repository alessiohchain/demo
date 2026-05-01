import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";

export type AuthResponse = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
};

export type CustomerDto = {
  id: number;
  email: string;
  displayName: string;
  memberSince: string;
};

export type RegisterPayload = {
  email: string;
  password: string;
  displayName: string;
};

export type LoginPayload = {
  email: string;
  password: string;
};

let accessToken: string | null = null;
let refreshHook: (() => Promise<string | null>) | null = null;

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function getAccessToken() {
  return accessToken;
}

export function registerRefreshHook(hook: () => Promise<string | null>) {
  refreshHook = hook;
}

export const api = axios.create({
  baseURL: "/api",
  withCredentials: true,
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken && !config.headers["Authorization"]) {
    config.headers["Authorization"] = `Bearer ${accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  (r) => r,
  async (error: AxiosError) => {
    const original = error.config as
      | (InternalAxiosRequestConfig & { _retry?: boolean })
      | undefined;
    if (
      error.response?.status === 401 &&
      original &&
      !original._retry &&
      !original.url?.includes("/auth/")
    ) {
      original._retry = true;
      const newToken = refreshHook ? await refreshHook() : null;
      if (newToken) {
        original.headers["Authorization"] = `Bearer ${newToken}`;
        return api.request(original);
      }
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  register: (payload: RegisterPayload) =>
    api.post<void>("/auth/register", payload).then((r) => r.data),
  login: (payload: LoginPayload) =>
    api.post<AuthResponse>("/auth/login", payload).then((r) => r.data),
  refresh: () =>
    api.post<AuthResponse>("/auth/refresh", null).then((r) => r.data),
  logout: () => api.post<void>("/auth/logout").then((r) => r.data),
};

export const meApi = {
  get: () => api.get<CustomerDto>("/me").then((r) => r.data),
};
