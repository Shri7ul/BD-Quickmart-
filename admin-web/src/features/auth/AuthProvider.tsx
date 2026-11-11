import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';

import { apiClient } from '../../lib/api/client';
import type { AuthenticatedUser } from '../../types';
import { AuthContext } from './auth-context';
import type { AuthContextValue, AuthStatus } from './auth-context';
import { loginRequest, refreshRequest } from './authApi';
import { isPrivileged } from './roles';
import { clearRefreshToken, readRefreshToken, writeRefreshToken } from './session';

interface AuthProviderProps {
  children: ReactNode;
}

/**
 * Owns the admin session. On mount it silently restores a session from the
 * refresh token; it also registers the single-flight refresh handler and the
 * auth-failure callback on the shared API client.
 *
 * Access tokens live only in memory (ApiClient); the refresh token lives in
 * sessionStorage. This is NOT HttpOnly-cookie protection and is XSS-exposed.
 */
export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<AuthenticatedUser | null>(null);
  const [status, setStatus] = useState<AuthStatus>('loading');
  const bootstrapStarted = useRef(false);

  const applyTokens = useCallback((accessToken: string, refreshToken: string) => {
    apiClient.setAuthToken(accessToken);
    writeRefreshToken(refreshToken);
  }, []);

  const clearSession = useCallback(() => {
    apiClient.clearAuthToken();
    clearRefreshToken();
    setUser(null);
  }, []);

  useEffect(() => {
    apiClient.setRefreshHandler(async () => {
      const refreshToken = readRefreshToken();
      if (!refreshToken) {
        return null;
      }
      try {
        const result = await refreshRequest(refreshToken);
        applyTokens(result.accessToken, result.refreshToken);
        return result.accessToken;
      } catch {
        clearRefreshToken();
        return null;
      }
    });

    apiClient.setOnAuthFailure(() => {
      clearRefreshToken();
      setUser(null);
      setStatus('unauthenticated');
    });

    return () => {
      apiClient.setRefreshHandler(null);
      apiClient.setOnAuthFailure(null);
    };
  }, [applyTokens]);

  useEffect(() => {
    // StrictMode invokes effects twice in development. Guard bootstrap so the
    // same refresh token is never rotated twice (which would look like replay).
    if (bootstrapStarted.current) {
      return;
    }
    bootstrapStarted.current = true;

    let cancelled = false;
    void (async () => {
      const refreshToken = readRefreshToken();
      if (!refreshToken) {
        if (!cancelled) {
          setUser(null);
          setStatus('unauthenticated');
        }
        return;
      }
      try {
        const result = await refreshRequest(refreshToken);
        if (cancelled) {
          return;
        }
        if (!isPrivileged(result.user.role)) {
          throw new Error('This account is not permitted to use the admin console.');
        }
        applyTokens(result.accessToken, result.refreshToken);
        setUser(result.user);
        setStatus('authenticated');
      } catch {
        if (!cancelled) {
          clearSession();
          setStatus('unauthenticated');
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [applyTokens, clearSession]);

  const login = useCallback(async (identifier: string, password: string) => {
    const result = await loginRequest(identifier, password);
    if (!isPrivileged(result.user.role)) {
      throw new Error(
        'This account is not permitted to use the admin console. '
          + 'Please use the customer or rider app instead.',
      );
    }
    applyTokens(result.accessToken, result.refreshToken);
    setUser(result.user);
    setStatus('authenticated');
  }, [applyTokens]);

  const logout = useCallback(async () => {
    // Best effort: revoke the refresh-token family on the server first.
    try {
      await apiClient.post<void>('/auth/logout');
    } catch {
      // Local cleanup must happen even if the server call fails.
    }
    clearSession();
    setStatus('unauthenticated');
  }, [clearSession]);

  const value = useMemo<AuthContextValue>(
    () => ({ user, status, login, logout }),
    [user, status, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
