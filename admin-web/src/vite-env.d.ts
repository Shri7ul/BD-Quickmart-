/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_WS_BASE_URL?: string;
  readonly VITE_REQUEST_TIMEOUT?: string;
  readonly VITE_IS_DEVELOPMENT?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
