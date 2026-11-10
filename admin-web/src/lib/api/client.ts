import axios, { type AxiosError, type AxiosInstance } from 'axios';

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
 * Centralized HTTP client for the BD QuickMart backend.
 *
 * Unwraps the standard `ApiResponse` envelope so callers receive the payload
 * directly, and converts transport failures into `ApiError`.
 */
export class ApiClient {
  private readonly http: AxiosInstance;
  private token: string | null = null;

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
      (error: AxiosError<ApiResponse<unknown>>) => {
        const body = error.response?.data;
        throw new ApiError(
          body?.message ?? error.message ?? 'Request failed',
          error.response?.status,
          body?.code,
        );
      },
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
