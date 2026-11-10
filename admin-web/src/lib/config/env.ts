/**
 * Environment-driven configuration for the admin web application.
 *
 * Values come from Vite's `import.meta.env`, which reads `.env` files at
 * build time. Only variables prefixed with `VITE_` are exposed to the client
 * bundle, so secrets must never be placed here.
 */
const env = import.meta.env;

export const appConfig = {
  /** Backend base URL including the `/api/v1` prefix. */
  apiBaseUrl: env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1',

  /** WebSocket endpoint for operational dashboards. */
  wsBaseUrl: env.VITE_WS_BASE_URL ?? 'ws://localhost:8080/ws/tracking',

  /** Request timeout in milliseconds. */
  requestTimeout: Number(env.VITE_REQUEST_TIMEOUT ?? 30000),

  /** Whether the app is running against a non-production backend. */
  isDevelopment: env.VITE_IS_DEVELOPMENT !== 'false',

  /** Application display name. */
  appName: 'BD QuickMart Admin',
} as const;

export type AppConfig = typeof appConfig;
