# BD QuickMart — Phase 1 Release APKs

This directory contains the installable Android release APKs for the two BD QuickMart
Phase 1 mobile apps. Both are built from the current repository source and point at the
**live production backend**.

| Application | APK file | Application ID | Size |
|---|---|---|---|
| BD QuickMart (Customer) | `BDQuickMart-Customer.apk` | `com.bdquickmart.customer_app` | 49.0 MB (51,425,669 bytes) |
| BD QuickMart Rider | `BDQuickMart-Rider.apk` | `com.bdquickmart.rider_app` | 48.1 MB (50,442,521 bytes) |

**Build date:** 2026-11-21

**SHA-256**
- `BDQuickMart-Customer.apk` — `dc891a20909b2445cb59a0c0b0ccab358f3a994ce45f276047eb9fde3ee32e52`
- `BDQuickMart-Rider.apk` — `81ba2801e56cfe07a4401ef20dea8ada86e8fce50970819c474cd85e85e7bc8e`

---

## Build configuration

- Flutter **3.35.1** (stable), Dart **3.9.0**.
- Release, universal APK (all supported ABIs).
- Built with:

  ```bash
  flutter build apk --release --dart-define-from-file=dart-defines/production.json.example
  ```

  Run from `customer-app/` and `rider-app/` respectively.

- Compile-time environment (`dart-defines/production.json.example`, identical in both apps):

  | Key | Value |
  |---|---|
  | `IS_DEVELOPMENT` | `false` |
  | `API_BASE_URL` | `https://bd-quickmart.onrender.com/api/v1` |
  | `WS_BASE_URL` | `wss://bd-quickmart.onrender.com/ws/tracking` |

- **Production API endpoint:** `https://bd-quickmart.onrender.com/api/v1`
- **WebSocket endpoint:** `wss://bd-quickmart.onrender.com/ws/tracking`

The older host `bd-quickmart-backend.onrender.com` is a **suspended** Render service
(HTTP 503) and is deliberately not used. Both APKs were string-verified to contain the
live host and **zero** occurrences of the suspended host.

`AppConfig` fails closed with a non-routable `.invalid` default when
`IS_DEVELOPMENT=false`, so a release build without the dart-defines file cannot silently
contact a development server.

---

## Installation instructions (Android)

1. Copy the APK to the Android phone (USB, email, cloud drive, or download from a
   trusted host).
2. On the phone, open **Settings → Security → Install unknown apps** and allow the app
   you will open the APK with (Files, Chrome, etc.) to install unknown apps.
3. Open the APK file and tap **Install**.
4. Android may show a warning because the APK is **debug-signed** (see Limitations).
   Acknowledge to complete installation.
5. Open the app:
   - **Customer app** — register or sign in, browse products, add to cart, check out
     with Cash on Delivery, then track orders from the Orders screen.
   - **Rider app** — register as a rider (account starts **PENDING**). After an admin
     approves the account, sign in, toggle **Online**, then open **Orders** to see
     assigned deliveries and work through pickup → out-for-delivery → delivered.

Both apps allow overriding the server URL at runtime from the sign-in screen, which is
useful for QA against a local backend; the baked-in default is the production endpoint.

---

## Known limitations

- **Debug-signed release builds.** No production keystore is configured, so the
  `release` build type currently signs with the debug key (the `TODO` in each app's
  `android/app/build.gradle.kts`). Installable on a physical device, but **not suitable
  for Google Play** and Android will display an "unknown developer" warning. No
  keystore or secret is committed to the repository.
- **Universal APKs** contain native libraries for all ABIs, hence the ~48–49 MB size.
  Splitting per-ABI (`--split-per-abi`) would reduce download size.
- **Phase 1 payments are Cash on Delivery (COD) only.** No online payment gateway.
- **Live order tracking uses a polling fallback.** The WebSocket handler is
  transport-only; customer order status refreshes periodically rather than relying on
  a fully implemented server push pipeline.
- **Rider availability and assignments are server-persisted** but delivered to the
  device by polling; there is no push notification when a new order is assigned.
- **Backend freshness.** The Render service must be redeployed with the Phase 1 APIs,
  and the `V2__commerce_schema.sql` migration must be applied to the production
  PostgreSQL database, before the full end-to-end workflow works in production.
  Render's free tier cold-starts, so the first request after idle may be slow.
- Location permission is used by the rider app for delivery tracking UX; a live GPS
  fix is not required to advance an order through its statuses.

---

## Testing notes

Verified in this repository before packaging (all green):

| Component | Command | Result |
|---|---|---|
| Backend | `mvn -B clean verify` | 76 tests, 0 failures, 0 errors, 9 skipped; BUILD SUCCESS |
| Admin web | `npm run lint` | 0 warnings, 0 errors |
| Admin web | `npm run build` | TypeScript + Vite build succeeded |
| Customer app | `flutter analyze` | No issues found |
| Customer app | `flutter test` | 14 tests passed |
| Rider app | `flutter analyze` | No issues found |
| Rider app | `flutter test` | 16 tests passed |
| Both APKs | string scan | live host present, suspended host absent |

Tests use in-memory storage / fake HTTP adapters and do not require a running backend.
The 9 skipped backend tests are PostgreSQL-dialect integration tests that need a real
local PostgreSQL instance (not available in this environment); they are not a product
defect.

**Live end-to-end verification** (admin creates category/product → customer checkout →
admin assigns rider → rider delivers → customer sees delivered status) still requires
deploying the Phase 1 backend/migration and is **not** covered by the local automated
suite. The admin web login and CORS preflight were previously verified against the live
host.
