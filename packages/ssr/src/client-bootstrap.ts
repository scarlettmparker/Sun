import { hydratePageData } from "./page-data";

/**
 * Minimal i18next-shaped instance the bootstrap initialises.
 */
export interface BootstrapI18n {
  /**
   * Registers a plugin (e.g. react-i18next).
   */
  use(plugin: unknown): unknown;
  /**
   * Initialises the instance with resources for the active locale.
   */
  init(options: Record<string, unknown>): Promise<unknown>;
}

declare global {
  interface Window {
    /**
     * Server-rendered page-data cache, populated by the postlude.
     */
    __serverCacheData__?: Record<string, Record<string, unknown>>;
    /**
     * Active locale code.
     */
    __locale__?: string;
    /**
     * Consolidated translations keyed by namespace.
     */
    __translations__?: Record<string, unknown>;
    /**
     * Hydration hook invoked by the postlude before client boot.
     */
    hydratePageDataFromPostlude?: typeof hydratePageData;
  }
}

/**
 * Wires the postlude hydration hook, initialises i18n from the server-shipped
 * translations, and hydrates the page-data cache. Resolves once i18n is ready.
 */
export async function initClientBootstrap(opts: {
  /**
   * The app's i18next instance, before or after .use(initReactI18next).
   */
  i18n: BootstrapI18n;
  /**
   * Override locale; defaults to window.__locale__.
   */
  locale?: string;
  /**
   * Override translations; defaults to window.__translations__.
   */
  translations?: Record<string, unknown>;
}): Promise<void> {
  window.hydratePageDataFromPostlude = hydratePageData;
  const locale = opts.locale ?? window.__locale__ ?? "en";
  const translations = opts.translations ?? window.__translations__ ?? {};

  await Promise.resolve(
    opts.i18n.init({
      lng: locale,
      resources: { [locale]: translations },
      interpolation: { escapeValue: false },
      react: { useSuspense: true },
    }),
  );
  const serverCacheData = window.__serverCacheData__ ?? {};
  if (Object.keys(serverCacheData).length > 0) {
    hydratePageData(serverCacheData);
    window.__serverCacheData__ = {};
  }
}
