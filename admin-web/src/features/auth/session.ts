/**
 * Browser-side session storage for the refresh token (Goal 02).
 *
 * The access token is deliberately kept only in memory (inside ApiClient).
 * The refresh token lives in sessionStorage so a page reload can silently
 * restore a session. This is NOT HttpOnly-cookie protection: both tokens are
 * readable by any script on the page and are therefore XSS-exposed. A
 * production deployment should move refresh tokens to a Secure, HttpOnly,
 * SameSite cookie with server-side rotation. See docs for the trade-off.
 */
const REFRESH_TOKEN_KEY = 'bdqm.refreshToken';

export function readRefreshToken(): string | null {
  try {
    return sessionStorage.getItem(REFRESH_TOKEN_KEY);
  } catch {
    return null;
  }
}

export function writeRefreshToken(token: string): void {
  try {
    sessionStorage.setItem(REFRESH_TOKEN_KEY, token);
  } catch {
    // Storage may be unavailable (private mode / blocked cookies). The
    // in-memory access token still works for the current page lifetime.
  }
}

export function clearRefreshToken(): void {
  try {
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  } catch {
    // Nothing to do when storage is unavailable.
  }
}
