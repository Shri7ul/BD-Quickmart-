import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios';

import { appConfig } from '../config/env';

/**
 * Standard response envelope returned by the BD QuickMart backend.
 */
export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T | null;
  path?: string;
  timestamp?: string;
  traceId?: string;
}

/**
 * Normalized error surfaced to the UI layer.
 */
export class ApiError extends Error {
  readonly status?: number;
  readonly code?: string;

  constructor(message: string, status?: number, code?: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

/**
 * Silent-refresh callback. It must exchange the stored refresh token for a
 * fresh access token and return that token, or null when the session cannot
 * be restored. The implementation lives in the auth feature so this module
 * stays React-free and free of circular imports.
 */
export type RefreshHandler = () => Promise<string | null>;

type RetryableConfig = InternalAxiosRequestConfig & { __retried?: boolean };

/**
 * Centralized HTTP client for the BD QuickMart backend.
 *
 * Unwraps the standard `ApiResponse` envelope so callers receive the payload
 * directly, converts transport failures into `ApiError`, and on a 401 performs
 * a single-flight silent refresh followed by exactly one retry. Concurrent
 * 401s share the same refresh promise instead of racing each other.
 */
export class ApiClient {
  private readonly http: AxiosInstance;
  private token: string | null = null;
  private refreshHandler: RefreshHandler | null = null;
  private onAuthFailure: (() => void) | null = null;
  private refreshPromise: Promise<string | null> | null = null;

  constructor() {
    this.http = axios.create({
      baseURL: appConfig.apiBaseUrl,
      timeout: appConfig.requestTimeout,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.http.interceptors.request.use((config) => {
      if (this.token) {
        config.headers.Authorization = `Bearer ${this.token}`;
      }
      return config;
    });

    this.http.interceptors.response.use(
      (response) => response,
      (error: AxiosError<ApiResponse<unknown>>) => this.handleError(error),
    );
  }

  /** Attaches the bearer token to subsequent requests. */
  setAuthToken(token: string): void {
    this.token = token;
  }

  /** Clears any attached credentials. */
  clearAuthToken(): void {
    this.token = null;
  }

  /** Registers the silent-refresh handler (React-free callback). */
  setRefreshHandler(handler: RefreshHandler | null): void {
    this.refreshHandler = handler;
  }

  /** Registers a callback invoked when the session cannot be restored. */
  setOnAuthFailure(handler: (() => void) | null): void {
    this.onAuthFailure = handler;
  }

  private async handleError(
    error: AxiosError<ApiResponse<unknown>>,
  ): Promise<AxiosResponse> {
    const config = error.config as RetryableConfig | undefined;

    if (
      error.response?.status === 401
      && config
      && !config.__retried
      && this.refreshHandler
    ) {
      config.__retried = true;
      const newToken = await this.runSingleFlightRefresh();
      if (newToken) {
        config.headers.Authorization = `Bearer ${newToken}`;
        return this.http.request(config);
      }
      this.token = null;
      this.onAuthFailure?.();
    }

    const body = error.response?.data;
    throw new ApiError(
      body?.message ?? error.message ?? 'Request failed',
      error.response?.status,
      body?.code,
    );
  }

  private runSingleFlightRefresh(): Promise<string | null> {
    if (!this.refreshPromise) {
      this.refreshPromise = (async () => {
        const token = this.refreshHandler ? await this.refreshHandler() : null;
        if (token) {
          this.token = token;
        }
        return token;
      })().finally(() => {
        this.refreshPromise = null;
      });
    }
    return this.refreshPromise;
  }

  async get<T>(path: string, params?: Record<string, unknown>): Promise<T> {
    return this.unwrap<T>(await this.http.get<ApiResponse<T>>(path, { params }));
  }

  async post<T>(path: string, data?: unknown): Promise<T> {
    return this.unwrap<T>(await this.http.post<ApiResponse<T>>(path, data));
  }

  async put<T>(path: string, data?: unknown): Promise<T> {
    return this.unwrap<T>(await this.http.put<ApiResponse<T>>(path, data));
  }

  async patch<T>(path: string, data?: unknown): Promise<T> {
    return this.unwrap<T>(await this.http.patch<ApiResponse<T>>(path, data));
  }

  async delete<T>(path: string): Promise<T> {
    return this.unwrap<T>(await this.http.delete<ApiResponse<T>>(path));
  }

  /** Extracts the payload from the standard envelope. */
  private unwrap<T>(response: { data: ApiResponse<T> }): T {
    const body = response.data;
    if (body?.data === null || body?.data === undefined) {
      return body as unknown as T;
    }
    return body.data;
  }
}

/** Shared API client instance used across the admin application. */
export const apiClient = new ApiClient();
