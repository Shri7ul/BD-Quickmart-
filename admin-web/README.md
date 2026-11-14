# React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some Oxlint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the Oxlint configuration

If you are developing a production application, we recommend enabling type-aware lint rules by installing `oxlint-tsgolint` and editing `.oxlintrc.json`:

```json
{
  "$schema": "./node_modules/oxlint/configuration_schema.json",
  "plugins": ["react", "typescript", "oxc"],
  "options": {
    "typeAware": true
  },
  "rules": {
    "react/rules-of-hooks": "error",
    "react/only-export-components": ["warn", { "allowConstantExport": true }]
  }
}
```

See the [Oxlint rules documentation](https://oxc.rs/docs/guide/usage/linter/rules) for the full list of rules and categories.

---

# BD QuickMart Admin Web — deployment & security notes

The generic Vite template text above is retained for reference; the notes below
describe the actual BD QuickMart project.

## Build and deploy (Vercel)

- **Root Directory** in the Vercel project settings must be `admin-web`
  (a dashboard setting — it is **not** a `vercel.json` field).
- `vercel.json` sets the Vite framework, `npm ci` / `npm run build`, `dist`
  output, SPA rewrite (`/(.*)` → `/index.html`) for `createBrowserRouter`
  deep links, and a small set of security headers.
- `VITE_API_BASE_URL`, `VITE_WS_BASE_URL`, `VITE_REQUEST_TIMEOUT`, and
  `VITE_IS_DEVELOPMENT` are baked into the JS bundle **at build time**. Set
  them in Vercel → Project → Settings → Environment Variables (Production)
  **before** building; changing them requires a redeploy. They are public —
  never put a secret in a `VITE_*` variable. See `.env.example`.
- Production must use `https://` / `wss://` endpoints.
- Add the Vercel production origin to the backend's
  `APP_CORS_ALLOWED_ORIGINS` (see `render.yaml`). `WebConfig` uses
  `setAllowCredentials(true)` with an explicit origin list, so `*` is invalid.

Verified locally: `npm run lint` (0 warnings/errors), `npm run build`
(success), and a build with marker `VITE_*` values confirmed the values are
embedded in the emitted bundle. No deployment has been performed.

## PRODUCTION SECURITY BLOCKER — refresh token in `sessionStorage`

**This design is XSS-exposed. It is not secure, and this document does not
claim otherwise.**

`src/features/auth/session.ts` stores the refresh token in `sessionStorage`
(key `bdqm.refreshToken`); the access token is held in memory in `ApiClient`.
Both are readable by any script running on the page. Any successful XSS — in
first-party code, a dependency, injected analytics, or a browser extension —
can read the refresh token and replay it to obtain fresh access tokens for the
full lifetime of that token family. `sessionStorage` is **not** HttpOnly and
provides no protection against script access; it only limits persistence to
the tab's lifetime. The `Strict-Transport-Security`/`X-Content-Type-Options`
headers in `vercel.json` reduce some attack surface but do **not** mitigate
this. A Content-Security-Policy is not configured (see below).

**Why it was not "fixed" here:** moving to a Secure/HttpOnly/SameSite cookie
requires changing the backend authentication contract — token issuance and
rotation would move from JSON response bodies to `Set-Cookie`, `/auth/refresh`
would become cookie-driven, CSRF protection would be required, and all three
clients (admin web, customer app, rider app) plus the refresh-token rotation
and revocation semantics would change. That would break the existing
authentication contract. It is therefore explicitly **not** implemented here.

**Status:** an open production security blocker, not an accepted-and-closed
risk. Before exposing real users, either:

1. Implement the HttpOnly-cookie migration as a dedicated, separately approved
   change across backend + all clients (with CSRF protection and a rotation/
   revocation redesign), or
2. Have the team explicitly record this as an **accepted risk** with a named
   owner, compensating controls (strict CSP, dependency auditing, no
   third-party scripts, short refresh lifetimes), and a remediation date.

Until one of those happens, do not describe the admin web authentication as
secure. See `docs/development/DEPLOYMENT.md` for the full risk register.

## Other known gaps (verified by inspection, not fixed here)

- **No rider-approval UI.** The `/riders` route is a role-gated placeholder
  `FeaturePage` ("Goal 07"); there are **no** approval/rejection API calls in
  this app. The backend endpoints exist at `/api/v1/admin/riders/*`. The
  rider approval workflow therefore **cannot** be verified through the admin
  console until that UI is implemented.
- **No automated test script.** `package.json` has no `test` script and no
  test runner dependency; verification today is lint + build only.
- **No CSP** configured in `vercel.json` (adding one needs a review of inline
  scripts/styles and `connect-src` for the API origin).
- **Dev fallback literal in bundle.** `env.ts` keeps
  `'http://localhost:8080/api/v1'` as a `??` fallback; it is dead code when
  `VITE_API_BASE_URL` is set, but the string is still present in the built JS
  bundle. Remove it (or gate it behind `import.meta.env.DEV`) if a strict
  "no dev URL in production bundle" policy is required.
- `VITE_WS_BASE_URL` is declared but no WebSocket client is implemented.
