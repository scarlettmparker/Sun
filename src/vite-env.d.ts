/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_DISCORD_REDIRECT_URI: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

export declare global {
  interface Window {
    hydratePageDataFromPostlude?: (
      initialData: Record<string, Record<string, unknown>>
    ) => void;
    __serverCacheData__?: Record<string, Record<string, unknown>>;
    __locale__?: string;
    __translations__?: Record<string, unknown>;
  }
}
