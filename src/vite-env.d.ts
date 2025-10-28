/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_DISCORD_REDIRECT_URI: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

export declare global {
  var __pageData__: Record<string, unknown> | undefined;
}
