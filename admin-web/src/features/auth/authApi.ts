import axios from 'axios';
import type { AxiosError } from 'axios';

import type { ApiResponse } from '../../lib/api/client';
import { appConfig } from '../../lib/config/env';
import type { AuthResponse } from '../../types';

/**
 * Unauthenticated auth calls use a bare axios instance with NO interceptors.
 * Keeping them separate from {@link ApiClient} avoids a refresh loop: a 401
 * from /auth/login or /auth/refresh must never trigger a token refresh.
 */
const http = axios.create({
  baseURL: appConfig.apiBaseUrl,
  timeout: appConfig.requestTimeout,
  headers: { 'Content-Type': 'application/json' },
});

function unwrap<T>(body: ApiResponse<T>): T {
  if (!body || body.success !== true || body.data === null || body.data === undefined) {
    throw new Error(body?.message ?? 'Unexpected API response');
  }
  return body.data;
}

/**
 * Converts a failed auth request into a clean Error carrying the backend's
 * human-readable message (e.g. "Invalid email/phone or password") instead of
 * axios' generic "Request failed with status code 401".
 */
function toAuthError(cause: unknown): Error {
  const axiosError = cause as AxiosError<ApiResponse<unknown>> | null;
  const message = axiosError?.response?.data?.message
    ?? (cause instanceof Error ? cause.message : null)
    ?? 'Unable to sign in. Please try again.';
  return new Error(message);
}

export async function loginRequest(
  identifier: string,
  password: string,
): Promise<AuthResponse> {
  try {
    const response = await http.post<ApiResponse<AuthResponse>>('/auth/login', {
      identifier,
      password,
    });
    return unwrap(response.data);
  } catch (cause) {
    throw toAuthError(cause);
  }
}

export async function refreshRequest(refreshToken: string): Promise<AuthResponse> {
  try {
    const response = await http.post<ApiResponse<AuthResponse>>('/auth/refresh', {
      refreshToken,
    });
    return unwrap(response.data);
  } catch (cause) {
    throw toAuthError(cause);
  }
}
